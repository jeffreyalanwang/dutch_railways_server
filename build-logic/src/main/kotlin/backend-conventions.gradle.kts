import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = parent!!.group

configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.bundles.test.junit5)
}

tasks.withType<Test> {
    useJUnitPlatform()
}