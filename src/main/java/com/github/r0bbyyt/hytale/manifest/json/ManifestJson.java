package com.github.r0bbyyt.hytale.manifest.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ManifestJson(
  @SerializedName("Group") String group,
  @SerializedName("Name") String name,
  @SerializedName("Version") String version,
  @SerializedName("Description") String description,
  @SerializedName("Authors") List<AuthorInfoJson> authors,
  @SerializedName("Website") String website,
  @SerializedName("Main") String main,
  @SerializedName("ServerVersion") String serverVersion,
  @SerializedName("Dependencies") Map<String, String> dependencies,
  @SerializedName("OptionalDependencies") Map<String, String> optionalDependencies,
  @SerializedName("LoadBefore") Map<String, String> loadBefore,
  @SerializedName("SubPlugins") List<ManifestJson> subPlugins,
  @SerializedName("DisabledByDefault") Boolean disabledByDefault,
  @SerializedName("IncludesAssetPack") Boolean includesAssetPack
) {
  public ManifestJson {
    Objects.requireNonNull(group, "group must be set");
    Objects.requireNonNull(name, "name must be set");
    Objects.requireNonNull(version, "version must be set");
    if (authors == null) {
      authors = List.of();
    }
    if (dependencies == null) {
      dependencies = Map.of();
    }
    if (optionalDependencies == null) {
      optionalDependencies = Map.of();
    }
    if (loadBefore == null) {
      loadBefore = Map.of();
    }
    if (subPlugins == null) {
      subPlugins = List.of();
    }
  }
}
