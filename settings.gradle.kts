rootProject.name = "velocity-punishment"
include("api")
include("plugin")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            library("velocity-api", "com.velocitypowered", "velocity-api").version("3.1.2-SNAPSHOT")
            library("luckperms-api", "net.luckperms", "api").version("5.4")
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").version("2.13.4")

            library("mariadb", "org.mariadb.jdbc", "mariadb-java-client").version("3.0.6")
            library("hikari", "com.zaxxer", "HikariCP").version("5.0.1")
            bundle("database", listOf("mariadb", "hikari"))

            val jUnitVersion = version("junit", "5.9.0")
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").version(jUnitVersion)
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").version(jUnitVersion)
        }
    }
}