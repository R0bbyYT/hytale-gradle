package com.github.r0bbyyt.hytale.util;

import org.gradle.api.plugins.ExtensionAware;

public final class Util {

  public static <T> T getExtension(Object obj, Class<T> extension) {
    if (!(obj instanceof ExtensionAware aware)) {
      throw new IllegalStateException("The given object " + obj + " is not an extension aware object.");
    }

    return aware.getExtensions().getByType(extension);
  }

}
