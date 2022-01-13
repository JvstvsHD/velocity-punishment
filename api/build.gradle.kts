plugins {
    java
}

group = "de.jvstvshd.punishment.velocity"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    withJavadocJar()
    withSourcesJar()
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

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}