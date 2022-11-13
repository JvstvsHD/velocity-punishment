plugins {
    java
    `java-library`
}

group = "de.jvstvshd.velocitypunishment"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    api(projects.api)
    api(libs.bundles.jackson)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}