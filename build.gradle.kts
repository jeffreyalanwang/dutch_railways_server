plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    tasks.withType<Test> {
        filter.isFailOnNoMatchingTests = false
    }
}