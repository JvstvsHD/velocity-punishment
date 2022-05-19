plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.jvstvshd.punishment.velocity"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
    compileOnly("net.luckperms:api:5.4")
    implementation(project(":api"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.4")
    implementation("com.zaxxer:HikariCP:5.0.1")
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
        archiveBaseName.set("velocity-punishment")
    }
    build {
        dependsOn(shadowJar)
    }
    register<Copy>("copyToServer") {
        val path = System.getenv("DEST") ?: ""
        if (path.isEmpty()) {
            println("no target directory was set")
            return@register
        }
        from(shadowJar)
        destinationDir = File(path.toString())
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

publishing {
    val repoUrl = System.getenv("REPO_URL")
    publications.create<MavenPublication>("maven") {
        artifact(project.tasks.getByName("jar"))
        artifact(project.tasks.getByName("sourcesJar"))
        artifact(project.tasks.getByName("javadocJar"))
        pom {
            name.set("Velocity Punishment API")
            description.set("API for punishing players via velocity")
            url.set("https://github.com/JvstvsHD/VelocityPunishment")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    name.set("JvstvsHD")
                    url.set("jvstvshd.de")
                }
            }
        }
    }
    repositories {
        maven {
            isAllowInsecureProtocol = true
            val url =
                uri(if ((version as String).endsWith("SNAPSHOT")) "${repoUrl}snapshots/" else "${repoUrl}releases/")
            println(url)
            this.url = url

            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
    }
}