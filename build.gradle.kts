import org.cadixdev.gradle.licenser.Licenser

plugins {
    java
    `maven-publish`
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "de.jvstvshd.punishment"
version = "1.0.0"

subprojects {
    apply {
        plugin<Licenser>()
        plugin<MavenPublishPlugin>()
    }
    repositories {
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://papermc.io/repo/repository/maven-public/")
        mavenCentral()
    }
}

allprojects {
    license {
        ignoreFailures(true)
        header(rootProject.file("HEADER.txt"))
        include("**/*.java")
        skipExistingHeaders(true)
        newLine(true)
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