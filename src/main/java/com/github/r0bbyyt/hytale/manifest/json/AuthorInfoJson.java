package com.github.r0bbyyt.hytale.manifest.json;

import com.google.gson.annotations.SerializedName;

public record AuthorInfoJson(
  @SerializedName("Name") String name,
  @SerializedName("Email") String email,
  @SerializedName("Url") String url
) {
}
