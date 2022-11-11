plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

version = rootProject.version
description = "A plugin handling all your needs for punishments on Velocity, based on the velocity-punishment-api."

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.api)
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.luckperms.api)
    implementation(libs.jackson.databind)
    implementation(libs.bundles.database)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
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
    val copyServerJar = task<Copy>("copyServerJar") {
        from(shadowJar)
        into(
            project.findProperty("velocity-plugins-directory")?.toString() ?: projectDir.toPath().resolve("build/libs")
                .toString()
        )
    }
    shadowJar {
        archiveBaseName.set("velocity-punishment-${rootProject.version}")
        finalizedBy(copyServerJar)
    }
    build {
        dependsOn(shadowJar)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}

/*
publishing {
    repositories {
        maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
            name = "ossrh"
            credentials {
                username = project.findProperty("sonatypeUsername") as String? ?: System.getenv("SONATYPE_USERNAME")
                password = project.findProperty("sonatypePassword") as String? ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
    publications {
        register<MavenPublication>(project.name) {
            from(components["java"])
            groupId = rootProject.group.toString().toLowerCase()
            artifactId = project.name.toLowerCase()
            version = project.version.toString()
            pom {
                name.set(project.name)
                description.set(project.description)
                developers {
                    developer {
                        name.set("JvstvsHD")
                    }
                }
                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }
                url.set("https://github.com/JvstvsHD/velocity-punishment")
                scm {
                    connection.set("scm:git:git://github.com/JvstvsHD/velocity-punishment.git")
                    url.set("https://github.com/JvstvsHD/velocity-punishment/tree/main")
                }
            }
        }
    }
}*/
