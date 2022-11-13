plugins {
    java
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}

group = "de.jvstvshd.velocitypunishment"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.karuslabs.com/repository/chimera-releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly(libs.paper.api)
    api(libs.brigadier)
    api(projects.pluginCommon)
}

tasks {
    val copyServerJar = task<Copy>("copyServerJar") {
        from(shadowJar)
        into(project.findProperty("plugins-directory") ?: "build/libs")
    }
    shadowJar {
        archiveBaseName.set("velocity-punishment-paper-extension")
        finalizedBy(copyServerJar)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

bukkit {
    main = "de.jvstvshd.velocitypunishment.paper.VelocityPunishmentPaperPlugin"
    name = "velocity-punishment-paper-extension"
    version = rootProject.version.toString()
    description = "A paper plugin complementing the velocity-punishment plugin for velocity for imposing mutes."
    apiVersion = "1.19"
}