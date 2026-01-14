package com.github.r0bbyyt.hytale.manifest;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public abstract class AuthorInfo {

  @Input
  public abstract Property<String> getName();

  @Input
  @Optional
  public abstract Property<String> getEmail();

  @Input
  @Optional
  public abstract Property<String> getUrl();
}
