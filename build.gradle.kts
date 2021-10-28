plugins {
    java
}

group = "de.jvstvshd"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://nexus.velocitypowered.com/repository/maven-public/")
    maven(url = "https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    mavenCentral()
}

dependencies {
    implementation("com.velocitypowered", "velocity-api", "3.0.1")
    annotationProcessor("com.velocitypowered", "velocity-api", "3.0.1")
    compileOnly("net.luckperms:api:5.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")

    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")
    implementation("com.zaxxer:HikariCP:5.0.0")
    compileOnly("dev.simplix:protocolize-api:2.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        destinationDirectory.set(File("C:\\Server\\TestNetzwerk\\Proxy\\plugins"))
    }
}