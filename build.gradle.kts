import org.cadixdev.gradle.licenser.Licenser

plugins {
    java
    `maven-publish`
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "de.jvstvshd.punishment.velocity"
version = "1.2.0-SNAPSHOT"

subprojects {
    apply {
        plugin<Licenser>()
        plugin<MavenPublishPlugin>()
    }

    license {
        header(rootProject.file("HEADER.txt"))
        include("**/*.java")
        newLine(true)
    }

    repositories {
        mavenCentral()
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}