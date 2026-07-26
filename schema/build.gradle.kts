plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.jeffeyalanwang.dutchrailways.api"
version = "GraphQL schema and supporting types"

dependencies {
    api(libs.geolatte.geom)
}
