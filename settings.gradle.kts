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
            library(
                "jackson-datatype-jsr310",
                "com.fasterxml.jackson.datatype",
                "jackson-datatype-jsr310"
            ).version("2.13.4")
            bundle("jackson", listOf("jackson-databind", "jackson-datatype-jsr310"))

            library("postgresql", "org.postgresql", "postgresql").version("42.5.0")
            library("hikari", "com.zaxxer", "HikariCP").version("5.0.1")
            bundle("database", listOf("postgresql", "hikari"))

            val jUnitVersion = version("junit", "5.9.1")
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(jUnitVersion)
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(jUnitVersion)

            library("paper-api", "io.papermc.paper", "paper-api").version("1.19.2-R0.1-SNAPSHOT")
            library("brigadier", "com.mojang", "brigadier").version("1.0.17")
        }
    }
}
include("paper-extension")
include("plugin-common")
