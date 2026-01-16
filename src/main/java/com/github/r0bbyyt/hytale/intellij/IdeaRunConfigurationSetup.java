package com.github.r0bbyyt.hytale.intellij;

import com.github.r0bbyyt.hytale.HytaleExtension;
import com.github.r0bbyyt.hytale.util.Util;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.Application;
import org.jetbrains.gradle.ext.IdeaExtPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfigurationContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IdeaRunConfigurationSetup {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdeaRunConfigurationSetup.class);
  private static final String RUN_CONFIG_NAME = "Hytale Server";

  private static final String ARG_ALLOW_OP = "--allow-op";
  private static final String ARG_AUTH_MODE = "--auth-mode";
  private static final String ARG_BIND = "--bind";
  private static final String ARG_DISABLE_SENTRY = "--disable-sentry";
  private static final String ARG_MODS = "--mods";
  private static final String ARG_ASSETS = "--assets";
  private static final String ASSETS_FILE = "Assets.zip";

  public void configure(Project project, HytaleExtension extension) {
    LOGGER.info("Starting IDEA run configuration setup for project: {}", project.getName());

    // Run configurations must be created on the root project's IdeaModel
    Project rootProject = project.getRootProject();
    rootProject.getPlugins().apply(IdeaExtPlugin.class);
    LOGGER.debug("Applied IdeaExtPlugin to root project: {}", rootProject.getName());

    IdeaModel ideaModel = rootProject.getExtensions().findByType(IdeaModel.class);
    if (ideaModel == null) {
      LOGGER.warn("IdeaModel is null on root project - cannot create run configuration");
      return;
    }
    LOGGER.debug("Found IdeaModel: {}", ideaModel);

    if (ideaModel.getProject() == null) {
      LOGGER.warn("IdeaModel.getProject() is null - cannot create run configuration");
      return;
    }
    LOGGER.debug("Found IdeaModel.project: {}", ideaModel.getProject());

    ProjectSettings projectSettings = Util.getExtension(ideaModel.getProject(), ProjectSettings.class);
    if (projectSettings == null) {
      LOGGER.warn("ProjectSettings extension is null - cannot create run configuration");
      return;
    }
    LOGGER.debug("Found ProjectSettings: {}", projectSettings);

    RunConfigurationContainer runConfigurations = Util.getExtension(projectSettings, RunConfigurationContainer.class);
    if (runConfigurations == null) {
      LOGGER.warn("RunConfigurationContainer is null - cannot create run configuration");
      return;
    }
    LOGGER.debug("Found RunConfigurationContainer: {}", runConfigurations);

    try {
      LOGGER.info("Creating run configuration '{}'", RUN_CONFIG_NAME);
      runConfigurations.create(RUN_CONFIG_NAME, Application.class, config -> {
        String mainClass = extension.getRunConfigMainClass().get();
        LOGGER.debug("Setting mainClass: {}", mainClass);
        config.setMainClass(mainClass);

        File workingDir = extension.getWorkingDirectory().get().getAsFile();
        workingDir.mkdirs();
        LOGGER.debug("Setting workingDirectory: {}", workingDir.getAbsolutePath());
        config.setWorkingDirectory(workingDir.getAbsolutePath());

        SourceSet mainSourceSet = project.getExtensions()
          .getByType(SourceSetContainer.class)
          .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String programArgs = this.buildProgramArguments(extension, mainSourceSet);
        LOGGER.debug("Setting programParameters: {}", programArgs);
        config.setProgramParameters(programArgs);
        config.moduleRef(project, mainSourceSet);

        LOGGER.info("Successfully configured run configuration '{}'", RUN_CONFIG_NAME);
      });
    } catch (Exception e) {
      LOGGER.error("Failed to create run configuration '{}': {}", RUN_CONFIG_NAME, e.getMessage(), e);
    }
  }

  private String buildProgramArguments(HytaleExtension extension, SourceSet sourceSet) {
    List<String> args = new ArrayList<>();

    if (extension.getAllowOp().getOrElse(false)) {
      args.add(ARG_ALLOW_OP);
    }

    if (extension.getAuthMode().isPresent()) {
      args.add(ARG_AUTH_MODE);
      args.add(extension.getAuthMode().get().getValue());
    }

    if (extension.getBindAddress().isPresent() && extension.getBindPort().isPresent()) {
      args.add(ARG_BIND);
      args.add(extension.getBindAddress().get() + ":" + extension.getBindPort().get());
    }

    if (extension.getDisableSentry().getOrElse(false)) {
      args.add(ARG_DISABLE_SENTRY);
    }

    File assetsFile = extension.getInstallationPath().get().file(ASSETS_FILE).getAsFile();
    args.add(ARG_ASSETS);
    args.add(assetsFile.getAbsolutePath());

    File resourceDir = sourceSet.getOutput().getResourcesDir();
    if (resourceDir != null && resourceDir.exists()) {
      args.add(ARG_MODS);
      args.add(resourceDir.getAbsolutePath());
    }

    return String.join(" ", args);
  }
}
