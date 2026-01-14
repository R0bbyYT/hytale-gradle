package com.github.r0bbyyt.hytale.manifest;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class PluginManifest implements Named {

  private final String name;

  @Inject
  public PluginManifest(String name) {
    this.name = name;
  }

  @Override
  @Input
  public String getName() {
    return this.name;
  }

  @Input
  @Optional
  public abstract Property<String> getGroup();

  @Input
  @Optional
  public abstract Property<String> getVersion();

  @Input
  @Optional
  public abstract Property<String> getDescription();

  @Nested
  public abstract NamedDomainObjectContainer<AuthorInfo> getAuthors();

  @Input
  @Optional
  public abstract Property<String> getWebsite();

  @Input
  public abstract Property<String> getMain();

  @Input
  @Optional
  public abstract Property<String> getServerVersion();

  @Input
  @Optional
  public abstract MapProperty<String, String> getDependencies();

  @Input
  @Optional
  public abstract MapProperty<String, String> getOptionalDependencies();

  @Input
  @Optional
  public abstract MapProperty<String, String> getLoadBefore();

  @Input
  @Optional
  public abstract Property<Boolean> getDisabledByDefault();

  @Input
  @Optional
  public abstract Property<Boolean> getIncludesAssetPack();

  @Nested
  public abstract NamedDomainObjectContainer<PluginManifest> getSubPlugins();

  @Inject
  protected abstract ObjectFactory getObjectFactory();

  public void authors(Action<? super NamedDomainObjectContainer<AuthorInfo>> action) {
    action.execute(this.getAuthors());
  }

  public void subPlugins(Action<? super NamedDomainObjectContainer<PluginManifest>> action) {
    action.execute(this.getSubPlugins());
  }
}
