plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.jvstvshd.punishment"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
    compileOnly("net.luckperms:api:5.3")
    implementation(project(":api"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")

    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")
    implementation("com.zaxxer:HikariCP:5.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        minimize()
    }
    build {
        dependsOn(shadowJar)
    }
}

