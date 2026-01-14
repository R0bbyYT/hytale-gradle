plugins {
  id("java")
  id("java-gradle-plugin")
  id("maven-publish")
}

group = "com.github.r0bbyyt"
version = findProperty("releaseVersion") as String? ?: "1.0-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("org.vineflower:vineflower:1.11.2")
  implementation("com.google.code.gson:gson:2.13.2")
  implementation("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:1.3")

  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
  plugins {
    create("hytalePlugin") {
      id = "com.github.r0bbyyt.hytale"
      implementationClass = "com.github.r0bbyyt.hytale.HytalePlugin"
    }
  }
}

tasks.test {
  useJUnitPlatform()
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}
