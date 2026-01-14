package com.github.r0bbyyt.hytale;

import com.github.r0bbyyt.hytale.intellij.IdeaRunConfigurationSetup;
import com.github.r0bbyyt.hytale.task.GenerateManifestTask;
import com.github.r0bbyyt.hytale.task.SetupServerDependencyTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

import java.io.IOException;
import java.nio.file.Path;

public class HytalePlugin implements Plugin<Project> {

  public static final String DEFAULT_GROUP = "hytale";

  public static final String TASK_GENERATE_MANIFEST = "generateManifest";
  public static final String TASK_SETUP_SERVER_DEPENDENCY = "setupServerDependency";
  public static final String TASK_PROCESS_RESOURCES = "processResources";
  public static final String TASK_COMPILE_JAVA = "compileJava";

  public static final String SERVER_SUBDIRECTORY = "Server";
  public static final String SERVER_JAR_NAME = "HytaleServer.jar";
  public static final String GENERATED_RESOURCES_DIR = "generated/resources";
  public static final String HYTALE_CACHE_DIR = ".gradle/hytale";
  public static final String LOCAL_REPO_SUBDIR = "repo";
  public static final String VERSION_CACHE_SUBDIR = "cache";

  public static final String JAVA_PLUGIN_ID = "java";
  public static final String IDEA_PLUGIN_ID = "idea";

  public static final String CONFIGURATION_IMPLEMENTATION = "implementation";
  public static final String VERSION_FALLBACK = "+";
  public static final String HYTALE_REPO_NAME = "HytaleLocalRepo";


  @Override
  public void apply(Project project) {
    PluginManager pluginManager = project.getPluginManager();
    pluginManager.apply(IdeaExtPlugin.class);

    HytaleExtension extension = project.getExtensions().create(
      HytaleExtension.NAME,
      HytaleExtension.class
    );

    TaskContainer tasks = project.getTasks();
    TaskProvider<GenerateManifestTask> generateManifest = tasks.register(
      TASK_GENERATE_MANIFEST,
      GenerateManifestTask.class,
      task -> {
        task.setGroup(DEFAULT_GROUP);
        task.setDescription("Generates manifest.json from plugin configurations");
        task.getPlugins().addAll(extension.getPlugins());
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir(GENERATED_RESOURCES_DIR));
      }
    );

    Path hytaleCacheDir = project.getRootDir().toPath().resolve(HYTALE_CACHE_DIR);
    Path repoDir = hytaleCacheDir.resolve(LOCAL_REPO_SUBDIR);
    Path versionCacheFilePath = hytaleCacheDir.resolve(VERSION_CACHE_SUBDIR).resolve(SetupServerDependencyTask.VERSION_CACHE_FILE);

    TaskProvider<SetupServerDependencyTask> setupServerDependency = tasks.register(
      TASK_SETUP_SERVER_DEPENDENCY,
      SetupServerDependencyTask.class,
      task -> {
        task.setGroup(DEFAULT_GROUP);
        task.setDescription("Sets up the Hytale server JAR as a Maven dependency");
        task.getInstallationPath().set(extension.getInstallationPath());
        task.getServerSubdirectory().set(SERVER_SUBDIRECTORY);
        task.getServerJarName().set(SERVER_JAR_NAME);
        task.getRepositoryDirectory().set(repoDir.toFile());
        task.getVersionCacheFile().set(versionCacheFilePath.toFile());
      }
    );
    RepositoryHandler repositories = project.getRepositories();
    repositories.maven(repo -> {
      repo.setName(HYTALE_REPO_NAME);
      repo.setUrl(repoDir.toUri());
    });

    pluginManager.withPlugin(JAVA_PLUGIN_ID, appliedPlugin -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
      mainSourceSet.getResources().srcDir(generateManifest.map(GenerateManifestTask::getOutputDirectory));

      tasks.named(TASK_PROCESS_RESOURCES, ProcessResources.class, task -> task.dependsOn(generateManifest));
      tasks.named(TASK_COMPILE_JAVA, task -> task.dependsOn(setupServerDependency));
      project.afterEvaluate(p -> this.configureServerDependency(p, versionCacheFilePath));
    });

    pluginManager.withPlugin(IDEA_PLUGIN_ID, appliedPlugin -> {
      IdeaRunConfigurationSetup ideaSetup = new IdeaRunConfigurationSetup();
      project.afterEvaluate(p -> ideaSetup.configure(p, extension));
    });
  }


  private void configureServerDependency(Project project, Path versionCacheFile) {
    var logger = project.getLogger();
    String version;
    try {
      version = SetupServerDependencyTask.readCachedVersion(versionCacheFile);
    } catch (IOException e) {
      version = null;
    }

    if (version == null) {
      logger.info("Server version not cached yet, dependency will be resolved after setupServerDependency task runs");
      version = VERSION_FALLBACK;
    }

    String dependencyNotation = SetupServerDependencyTask.GROUP_ID
                                + ":"
                                + SetupServerDependencyTask.ARTIFACT_ID
                                + ":"
                                + version;

    project.getDependencies().add(CONFIGURATION_IMPLEMENTATION, dependencyNotation);
    logger.lifecycle("Added server dependency: {}", dependencyNotation);
  }
}
