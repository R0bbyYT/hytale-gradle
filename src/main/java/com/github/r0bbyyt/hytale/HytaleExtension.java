package com.github.r0bbyyt.hytale;

import com.github.r0bbyyt.hytale.manifest.PluginManifest;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;
import java.net.InetSocketAddress;

public abstract class HytaleExtension {

  public static final String NAME = "hytale";
  public static final String DEFAULT_MAIN_CLASS = "com.hypixel.hytale.Main";
  public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
  public static final int DEFAULT_BIND_PORT = 25565;
  public static final String DEFAULT_WORKING_DIR = "run";

  @Inject
  public HytaleExtension(ProjectLayout layout) {
    this.getAllowOp().convention(false);
    this.getAuthMode().convention(AuthMode.AUTHENTICATED);
    this.getBindAddress().convention(DEFAULT_BIND_ADDRESS);
    this.getBindPort().convention(DEFAULT_BIND_PORT);
    this.getDisableSentry().convention(true);
    this.getRunConfigMainClass().convention(DEFAULT_MAIN_CLASS);
    this.getWorkingDirectory().convention(layout.getProjectDirectory().dir(DEFAULT_WORKING_DIR));
  }

  public abstract DirectoryProperty getInstallationPath();

  public abstract Property<Boolean> getAllowOp();

  public abstract Property<AuthMode> getAuthMode();

  public abstract Property<String> getBindAddress();

  public abstract Property<Integer> getBindPort();

  public abstract Property<Boolean> getDisableSentry();

  public abstract Property<String> getRunConfigMainClass();

  public abstract DirectoryProperty getWorkingDirectory();

  @Nested
  public abstract NamedDomainObjectContainer<PluginManifest> getPlugins();

  public void plugins(Action<? super NamedDomainObjectContainer<PluginManifest>> action) {
    action.execute(this.getPlugins());
  }

  public InetSocketAddress getBindSocketAddress() {
    return new InetSocketAddress(this.getBindAddress().get(), this.getBindPort().get());
  }

  public enum AuthMode {
    AUTHENTICATED("authenticated"),
    OFFLINE("offline");

    private final String value;

    AuthMode(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }
}
