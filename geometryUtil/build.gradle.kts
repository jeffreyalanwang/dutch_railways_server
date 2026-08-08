plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
    id("common-compatibility-conventions")
}

group = "com.jeffreyalanwang.util"
description = "Tools for manipulation of GeoLatte geometry objects"

dependencies {
    api(libs.geolatte.geom)
    implementation(libs.bundles.proj4j)

    testFixturesImplementation(libs.proj4j)
    testFixturesImplementation(libs.bundles.test.junit)

    testImplementation(libs.bundles.test.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
