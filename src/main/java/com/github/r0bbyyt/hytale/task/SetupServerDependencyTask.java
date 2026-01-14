package com.github.r0bbyyt.hytale.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SetupServerDependencyTask extends DefaultTask {

  public static final String GROUP_ID = "com.hypixel.hytale";
  public static final String ARTIFACT_ID = "server";
  public static final String MANIFEST_VERSION_ATTRIBUTE = "Implementation-Version";
  public static final String VERSION_CACHE_FILE = "server-version.txt";
  public static final String SOURCES_CLASSIFIER = "sources";
  public static final String INCLUDED_PACKAGE_PREFIX = "com/hypixel/";

  public static final String DECOMPILER_HEAP_SIZE = "4g";
  public static final String TEMP_DIR_NAME = ".tmp";
  public static final String FILTERED_JAR_NAME = "filtered.jar";
  public static final String DECOMPILED_DIR_NAME = "decompiled";
  public static final String CLASS_EXTENSION = ".class";
  public static final String JAVA_EXTENSION = ".java";
  public static final String INDENT_STRING = "    ";

  public static String readCachedVersion(Path versionFile) throws IOException {
    if (Files.exists(versionFile)) {
      return Files.readString(versionFile).trim();
    }
    return null;
  }

  @InputDirectory
  public abstract DirectoryProperty getInstallationPath();

  @Input
  public abstract Property<String> getServerSubdirectory();

  @Input
  public abstract Property<String> getServerJarName();

  @OutputDirectory
  public abstract DirectoryProperty getRepositoryDirectory();

  @OutputFile
  public abstract RegularFileProperty getVersionCacheFile();

  @Inject
  public abstract WorkerExecutor getWorkerExecutor();

  @TaskAction
  public void setup() throws IOException {
    var logger = this.getLogger();
    Path installPath = this.getInstallationPath().get().getAsFile().toPath();
    Path serverJarPath = installPath
      .resolve(this.getServerSubdirectory().get())
      .resolve(this.getServerJarName().get());

    if (!Files.exists(serverJarPath)) {
      throw new GradleException("Server JAR not found at: " + serverJarPath);
    }

    String version = this.extractVersion(serverJarPath);
    logger.lifecycle("Detected server version: {}", version);

    Path versionFile = this.getVersionCacheFile().get().getAsFile().toPath();
    Files.createDirectories(versionFile.getParent());
    Files.writeString(versionFile, version);

    Path repoDir = this.getRepositoryDirectory().get().getAsFile().toPath();
    Path artifactDir = this.prepareArtifactDirectory(repoDir, version);

    this.publishMainJar(serverJarPath, artifactDir, version);
    this.publishPom(artifactDir, version);

    this.getWorkerExecutor().processIsolation(spec -> {
      spec.forkOptions(fork -> fork.setMaxHeapSize(DECOMPILER_HEAP_SIZE));
    }).submit(DecompileAction.class, parameters -> {
      parameters.getServerJar().set(serverJarPath.toFile());
      parameters.getArtifactDirectory().set(artifactDir.toFile());
      parameters.getVersion().set(version);
    });

    logger.lifecycle("Published {}:{}:{} to local repository (with sources)", GROUP_ID, ARTIFACT_ID, version);
  }

  private String extractVersion(Path jarPath) throws IOException {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      Manifest manifest = jarFile.getManifest();
      if (manifest == null) {
        throw new GradleException("No manifest found in server JAR");
      }

      String version = manifest.getMainAttributes().getValue(MANIFEST_VERSION_ATTRIBUTE);
      if (version == null || version.isBlank()) {
        throw new GradleException("No " + MANIFEST_VERSION_ATTRIBUTE + " found in server JAR manifest");
      }

      return version;
    }
  }

  private Path prepareArtifactDirectory(Path repoDir, String version) throws IOException {
    String groupPath = GROUP_ID.replace('.', '/');
    Path artifactDir = repoDir.resolve(groupPath).resolve(ARTIFACT_ID).resolve(version);
    Files.createDirectories(artifactDir);
    return artifactDir;
  }

  private void publishMainJar(Path jarPath, Path artifactDir, String version) throws IOException {
    String artifactBaseName = ARTIFACT_ID + "-" + version;
    Path targetJar = artifactDir.resolve(artifactBaseName + ".jar");
    Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
  }

  private void publishPom(Path artifactDir, String version) throws IOException {
    String artifactBaseName = ARTIFACT_ID + "-" + version;
    Path targetPom = artifactDir.resolve(artifactBaseName + ".pom");
    String pomContent = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
               xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <modelVersion>4.0.0</modelVersion>
          <groupId>%s</groupId>
          <artifactId>%s</artifactId>
          <version>%s</version>
          <packaging>jar</packaging>
      </project>
      """.formatted(GROUP_ID, ARTIFACT_ID, version);
    Files.writeString(targetPom, pomContent);
  }

  public interface DecompileParameters extends WorkParameters {
    RegularFileProperty getServerJar();

    DirectoryProperty getArtifactDirectory();

    Property<String> getVersion();
  }

  public static abstract class DecompileAction implements WorkAction<DecompileParameters> {

    @Override
    public void execute() {
      DecompileParameters params = this.getParameters();
      File serverJar = params.getServerJar().get().getAsFile();
      File artifactDir = params.getArtifactDirectory().get().getAsFile();
      String version = params.getVersion().get();

      try {
        Path artifactPath = artifactDir.toPath();
        Path tempDir = artifactPath.resolve(TEMP_DIR_NAME);
        Files.createDirectories(tempDir);
        Path filteredJar = tempDir.resolve(FILTERED_JAR_NAME);
        Path decompileDir = tempDir.resolve(DECOMPILED_DIR_NAME);
        Files.createDirectories(decompileDir);
        try {
          this.createFilteredJar(serverJar, filteredJar);
          this.decompile(filteredJar.toFile(), decompileDir.toFile());

          String sourcesJarName = ARTIFACT_ID + "-" + version + "-" + SOURCES_CLASSIFIER + ".jar";
          Path sourcesJar = artifactPath.resolve(sourcesJarName);
          this.createSourcesJar(decompileDir, sourcesJar);
        } finally {
          this.deleteDirectory(tempDir);
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to create sources JAR", e);
      }
    }

    private void createFilteredJar(File sourceJar, Path targetJar) throws IOException {
      try (JarFile jarFile = new JarFile(sourceJar);
           ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetJar.toFile()))) {

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();

          if (name.startsWith(INCLUDED_PACKAGE_PREFIX) && name.endsWith(CLASS_EXTENSION)) {
            zos.putNextEntry(new ZipEntry(name));
            try (InputStream is = jarFile.getInputStream(entry)) {
              is.transferTo(zos);
            }
            zos.closeEntry();
          }
        }
      }
    }

    private void decompile(File inputJar, File outputDir) {
      Map<String, Object> options = Map.of(
        IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1",
        IFernflowerPreferences.REMOVE_SYNTHETIC, "1",
        IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH, "1",
        IFernflowerPreferences.INDENT_STRING, INDENT_STRING
      );

      Fernflower decompiler = new Fernflower(
        new DirectoryResultSaver(outputDir),
        options,
        new PrintStreamLogger(System.out)
      );

      decompiler.addSource(inputJar);
      try {
        decompiler.decompileContext();
      } finally {
        decompiler.clearContext();
      }
    }

    private void createSourcesJar(Path sourceDir, Path targetJar) throws IOException {
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetJar.toFile()))) {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(JAVA_EXTENSION)) {
              String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
              zos.putNextEntry(new ZipEntry(entryName));

              try (FileInputStream fis = new FileInputStream(file.toFile())) {
                fis.transferTo(zos);
              }

              zos.closeEntry();
            }
            return FileVisitResult.CONTINUE;
          }
        });
      }
    }

    private void deleteDirectory(Path directory) throws IOException {
      Files.walkFileTree(directory, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}
