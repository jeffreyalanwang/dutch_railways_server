plugins {
    kotlin("jvm")
}

group = "com.jeffeyalanwang.dutchrailways.backend"
version = "0.0.1-SNAPSHOT"
description = "Dutch Railways Backend"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}