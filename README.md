# Velocity Punishment

Velocity punishment is a punishment plugin designed for
[velocity](https://velocitypowered.com).<br>
<b>Please not that this plugin is in its development stage at the moment and has not been release fully yet.

## Table of contents

1. [Plugin installation](#plugin-installation)
2. [API](#punishment-api)
    * [installation](#installation)

## Plugin installation

1. [Download the latest version of the plugin](https://github.com/JvstvsHD/VelocityPunishment/releases/latest) (only a
   pre-release)
2. Put the downloaded file into the ```plugins``` folder of your server.
3. (Re-)Start the server.

## Punishment API

### Installation

Replace ```{version}``` with the current version, e.g. 1.0.0. Note that the artifacts are not yet published.

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

```java
    VelocityPunishmentPlugin plugin;
        Optional<PluginContainer> pluginContainer=server.getPluginManager().getPlugin("velocity-punishment");
        if(pluginContainer.isEmpty()){
        return;
        }
        plugin=(VelocityPunishmentPlugin)pluginContainer.get().getInstance().orElseThrow(()->new NullPointerException("plugin 'velocity-punishment' was not found."));
```

More info coming soon....
