plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.velocity.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    withJavadocJar()
    withSourcesJar()
}

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
            groupId = project.group.toString().toLowerCase()
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
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}