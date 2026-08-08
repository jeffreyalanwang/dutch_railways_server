plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "dutch_railways_server"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
    versionCatalogs.create("libs").from(files("./build-logic/libs.versions.toml"))
}

includeBuild("build-logic")

include("server")
include("routeQuery")
include("schema")

