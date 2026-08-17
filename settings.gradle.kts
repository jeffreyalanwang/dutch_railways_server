plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
    versionCatalogs.create("libs").from(files("../build-logic/libs.versions.toml"))
}

includeBuild("../build-logic")
includeBuild("../lib")
include("database")
include("routeQuery")
include("server")
