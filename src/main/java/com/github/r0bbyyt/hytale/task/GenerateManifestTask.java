package com.github.r0bbyyt.hytale.task;

import com.github.r0bbyyt.hytale.manifest.AuthorInfo;
import com.github.r0bbyyt.hytale.manifest.PluginManifest;
import com.github.r0bbyyt.hytale.manifest.json.AuthorInfoJson;
import com.github.r0bbyyt.hytale.manifest.json.ManifestJson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GenerateManifestTask extends DefaultTask {

  public static final String SINGLE_MANIFEST_FILE = "manifest.json";
  public static final String MULTI_MANIFEST_FILE = "manifests.json";

  @Nested
  public abstract NamedDomainObjectContainer<PluginManifest> getPlugins();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  @TaskAction
  public void generate() throws IOException {
    Path outputDir = this.getOutputDirectory().get().getAsFile().toPath();
    Files.createDirectories(outputDir);

    var logger = this.getLogger();
    Gson gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

    NamedDomainObjectContainer<PluginManifest> plugins = this.getPlugins();

    if (plugins.isEmpty()) {
      logger.lifecycle("No plugins configured, skipping manifest generation");
      return;
    }

    if (plugins.size() == 1) {
      PluginManifest manifest = plugins.iterator().next();
      ManifestJson json = this.toJson(manifest);

      Path outputFile = outputDir.resolve(SINGLE_MANIFEST_FILE);
      try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
        gson.toJson(json, writer);
      }
      logger.lifecycle("Generated {} for plugin: {}", SINGLE_MANIFEST_FILE, manifest.getName());
    } else {
      List<ManifestJson> manifestsList = new ArrayList<>();
      for (PluginManifest manifest : plugins) {
        manifestsList.add(this.toJson(manifest));
      }

      Path outputFile = outputDir.resolve(MULTI_MANIFEST_FILE);
      try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
        gson.toJson(manifestsList, writer);
      }
      logger.lifecycle("Generated {} for {} plugins", MULTI_MANIFEST_FILE, plugins.size());
    }
  }

  private ManifestJson toJson(PluginManifest manifest) {
    List<AuthorInfoJson> authors = new ArrayList<>();
    for (AuthorInfo author : manifest.getAuthors()) {
      authors.add(this.toJson(author));
    }

    Map<String, String> dependencies = manifest.getDependencies().getOrElse(Map.of());
    Map<String, String> optionalDependencies = manifest.getOptionalDependencies().getOrElse(Map.of());
    Map<String, String> loadBefore = manifest.getLoadBefore().getOrElse(Map.of());

    List<ManifestJson> subPlugins = new ArrayList<>();
    for (PluginManifest subPlugin : manifest.getSubPlugins()) {
      subPlugins.add(this.toJson(subPlugin));
    }

    return new ManifestJson(
      manifest.getGroup().get(),
      manifest.getName(),
      manifest.getVersion().get(),
      manifest.getDescription().getOrNull(),
      authors,
      manifest.getWebsite().getOrNull(),
      manifest.getMain().getOrNull(),
      manifest.getServerVersion().getOrNull(),
      dependencies,
      optionalDependencies,
      loadBefore,
      subPlugins,
      manifest.getDisabledByDefault().getOrNull(),
      manifest.getIncludesAssetPack().getOrNull()
    );
  }

  private AuthorInfoJson toJson(AuthorInfo author) {
    return new AuthorInfoJson(
      author.getName().get(),
      author.getEmail().getOrNull(),
      author.getUrl().getOrNull()
    );
  }
}
