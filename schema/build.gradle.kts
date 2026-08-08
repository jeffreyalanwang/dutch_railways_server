plugins {
    alias(libs.plugins.kotlin.jvm)
    id("common-compatibility-conventions")
}

group = "com.jeffreyalanwang.dutchrailways.api"
description = "GraphQL schema and supporting types"

dependencies {
    implementation(project(":geometryUtil"))
    api(libs.geolatte.geom)
}