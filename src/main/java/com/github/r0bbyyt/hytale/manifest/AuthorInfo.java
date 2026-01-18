package com.github.r0bbyyt.hytale.manifest;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class AuthorInfo implements Named {

  private final String name;

  @Inject
  public AuthorInfo(String name) {
    this.name = name;
  }

  @Override
  @Input
  public String getName() {
    return this.name;
  }

  @Input
  @Optional
  public abstract Property<String> getEmail();

  @Input
  @Optional
  public abstract Property<String> getUrl();
}
