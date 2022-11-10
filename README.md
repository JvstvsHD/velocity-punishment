# Velocity Punishment

Velocity punishment is a punishment plugin designed for
[velocity](https://velocitypowered.com).<br>
<b>Please not that this plugin is in its development stage at the moment and has not been release fully yet.</b>

## Table of contents

1. [Plugin installation](#plugin-installation)
2. [Duration](#duration)
3. [Commands](#commands)
4. [API](#punishment-api)
    * [installation](#installation)
    * [usage](#usage)

## Plugin installation

1. [Download the latest version of the plugin](https://github.com/JvstvsHD/VelocityPunishment/releases/latest)
2. Put the downloaded file into the ```plugins``` folder of your server.
3. (Re-)Start the server.

## Mutes

With the 1.19.1, minecraft's chat system was
changed ([detailed explanation](https://gist.github.com/kennytv/ed783dd244ca0321bbd882c347892874)).
Since then, it is no longer possible to deny chat messages in the ChatEvent of Velocity due to the signed chat messages.
This is why the chat listener does not block any messages anymore which means mutes are effectively useless. A solution
to this problem is developing an extension plugin for the actual game servers where cancelling these messages is still
possible.<br>
Related issue: #6<br>
For further information about 1.19.1, please refer to
the [official release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-19-1)

## Commands

<b>legend:</b>

- \<arg\> means the argument is required
- \[arg\] means the argument is optional
- player as argument name means the a player name OR uuid is required
- reason means a reason with legacy color codes
- duration as argument name means a [duration](#duration)

### Command overview

- **/ban \<player\> \[reason\]** bans a player permanently for the given or the default reason
- **/mute \<player\> \[reason\]** mutes a player permanently for the given or the default reason
- **/punishment \<playerinfo\> \<player\>** shows information about a player's punishments
- **/punishment <cancel|change|info|remove> \<punishment id\>** cancels/removes, changes or shows information about the
  given punishment(must be a uuid)
- **/tempban <player> <duration> [reason]** bans a player for the given duration for the given or the default reason
- **/tempmute <player> <duration> [reason]** mutes a player for the given duration for the given or the default reason
- **/unban <player>** unbans the given player
- **/unmute <player>** unmutes the given player

### Duration

To be parsed by `PunishmentDuration#parse(String)`, a string must follow this scheme:<br>
[0-9][s, m, h, d]<br>
s - second(s)<br>
m - minute(s)<br>
h - hour(s)<br>
d - day(s)<br>
These value can be composed, all of them can be omitted.<br>
Example: <b>1d12h15m30s</b> means a duration of 1 day, 12 hours, 15 minutes and 30 seconds.

## Punishment API

### Installation

Replace ```{version}``` with the current version, e.g. 1.0.0. Note that the artifacts are not yet published. This
section is currently subject to change.

#### Gradle (kotlin)

```kotlin
repositories {
   mavenCentral()
}

depenencies {
   implementation("de.jvstvshd.punishment.velocity:api:{version}")
}
```

#### Gradle (groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
   implementation 'de.jvstvshd.punishment.velocity:api:{version}'
}
```

#### Maven

```xml

<dependencies>
   <dependency>
      <groupId>de.jvstvshd.punishment.velocity</groupId>
      <artifactId>api</artifactId>
      <version>{version}</version>
   </dependency>
</dependencies>
```

### Usage

#### Obtaining an instance of the api

If the [plugin](#plugin-installation) is used, you can obtain an instance of the api using the following snippet:

```java
    try {
        VelocityPunishment api = (VelocityPunishment) server.getPluginManager().getPlugin("velocity-punishment").orElseThrow().getInstance().orElseThrow();
    } catch(NoSuchElementException e) {
        logger.error("Punishment API is not available");
    }
```

#### Punishing a player

All punishments are imposed via the punishment manager (obtainable via VelocityPunishment#getPunishmentManager). For
example, banning a player could be done this way:

```java
    PunishmentManager punishmentManager = api.getPunishmentManager();
    //temporary ban:
    Ban temporaryBan = punishmentManager.createBan(uuid, Component.text("You are banned from this server.").color(NamedTextColor.RED), PunishmentDuration.parse("1d"));//1d equals 1 day, the duration is relative to the current time until the punishment is imposed.
    //permanent ban:
    Ban permanentBan = punishmentManager.createPermanentBan(uuid2, Component.text("You are banned permanently from this server").color(NamedTextColor.RED))
    //To finally punish the player, use Punishment#punish which will return a CompletableFuture with the punishment was imposed
    temporaryBan.punish().whenCompleteAsync((ban,throwable) -> {
    if (throwable != null) {
        logger.error("Error punishing player", throwable);
        return;
    }
    logger.info("The player was successfully banned. Punishment id: " + ban.getPunishmentUuid());
    });
```

Muting a player is similar, just replace 'ban' with 'mute'.