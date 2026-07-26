plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.jeffreyalanwang.dutchrailways.backend"
description = "Implementation of transit route planning algorithms"

dependencies {
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
