# Velocity Punishment

Velocity punishment is a punishment plugin designed for
[velocity](https://velocitypowered.com). The plugin is in the development stage at the moment. There will be a
downloadable jar file soon.

## Table of contents

1. [Plugin installation](#plugin-installation)
2. [API](#punishment-api)
    * [installation](#installation)

## Plugin installation

1. [Download the latest version of the plugin](https://github.com/JvstvsHD/VelocityPunishment/releases/latest)
2. Put the downloaded file into the ```plugins``` folder of your server.
3. (Re-)Start the server.

## Punishment API

### Obtaining the plugin instance

```java
public void setupPunishmentManager(ProxyServer server) {
        VelocityPunishmentPlugin plugin;
        Optional<PluginContainer> pluginContainer = server.getPluginManager().getPlugin("velocity-punishment");
        if(pluginContainer.isEmpty()){
        return;
        }
        plugin = (VelocityPunishmentPlugin)pluginContainer.get().getInstance().orElseThrow();
    }
```

This plugin also contains an API which partially exist yet.

### Installation

Replace ```{version}``` with the current version, e.g. 1.0.0.

#### Gradle (kotlin)

```kotlin
repositories {
    mavenCentral()
}

depenencies {
    implementation("de.jvstvshd.punishment:velocitypunishment:{version}")
}
```

#### Gradle (groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'de.jvstvshd.punishment:velocitypunishment:{version}'
}
```

#### Maven

```xml

<dependencies>
    <dependency>
        <groupId>de.jvstvshd.punishment</groupId>
        <artifactId>velocitypunishment</artifactId>
        <version>{version}</version>
    </dependency>
</dependencies>
```

### Usage

Coming soon.