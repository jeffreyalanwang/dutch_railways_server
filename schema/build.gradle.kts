plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.jeffeyalanwang.dutchrailways.api"
version = "GraphQL schema and supporting types"

kotlin {
    // This module may need to run on a lower JVM version
    // for e.g. Android application compatibility
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add(
            "-Xjdk-release=17",
        )
    }
}

dependencies {
    api(libs.geolatte.geom)
}
