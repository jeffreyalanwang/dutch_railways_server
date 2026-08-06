plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.jeffreyalanwang.dutchrailways.backend"
description = "Implementation of transit route planning algorithms"

kotlin {
    compilerOptions {
        jvmToolchain(21)
    }
}

dependencies {
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.bundles.test.junit5)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}