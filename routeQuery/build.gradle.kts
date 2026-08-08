plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = parent!!.group
description = "Implementation of transit route planning algorithms"

kotlin {
    compilerOptions {
        jvmToolchain(21)
    }
}

dependencies {
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.bundles.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}