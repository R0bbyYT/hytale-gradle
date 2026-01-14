# Hytale Gradle Plugin

A Gradle plugin for Hytale server mod development. This plugin simplifies the setup process by automatically configuring dependencies, generating plugin manifests, and creating IntelliJ IDEA run configurations.

## Features

- Automatically sets up the Hytale server JAR as a Maven dependency
- Decompiles server classes to provide source navigation in your IDE
- Generates `manifest.json` for your plugin configurations
- Creates IntelliJ IDEA run configurations for easy server launching
- Configurable server options (auth mode, bind address, op permissions, etc.)

## Installation

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("com.github.r0bbyyt.hytale") version "1.0.0"
}
```

Or using the legacy plugin application:

```kotlin
buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.github.r0bbyyt:hytale-gradle:1.0.0")
    }
}

apply(plugin = "com.github.r0bbyyt.hytale")
```

## Configuration

Configure the plugin in your `build.gradle.kts`:

```kotlin
hytale {
    // Required: Path to your Hytale installation
    installationPath.set(file("${System.getProperty("user.home")}/AppData/Roaming/Hytale/install/release/package/game/latest"))

    // Optional: Server configuration
    allowOp.set(false)                    // Allow operator commands (default: false)
    authMode.set(AuthMode.AUTHENTICATED)  // Auth mode: AUTHENTICATED or OFFLINE (default: AUTHENTICATED)
    bindAddress.set("0.0.0.0")            // Server bind address (default: "0.0.0.0")
    bindPort.set(25565)                   // Server bind port (default: 25565)
    disableSentry.set(true)               // Disable Sentry error reporting (default: true)
    workingDirectory.set(file("run"))     // Server working directory (default: project/run)

    // Plugin manifest configuration
    plugins {
        create("myPlugin") {
            group.set("com.example")
            version.set("1.0.0")
            description.set("My awesome Hytale plugin")
            main.set("com.example.MyPlugin")

            authors {
                create("author1") {
                    name.set("Your Name")
                    email.set("your@email.com")
                    url.set("https://github.com/yourusername")
                }
            }
        }
    }
}
```

## Tasks

| Task | Description |
|------|-------------|
| `setupServerDependency` | Sets up the Hytale server JAR as a Maven dependency with decompiled sources |
| `generateManifest` | Generates `manifest.json` from plugin configurations |

## Requirements

- Java 25 or higher
- Gradle 9.0 or higher
- A valid Hytale installation

## License

This project is provided as-is for Hytale mod development purposes.
