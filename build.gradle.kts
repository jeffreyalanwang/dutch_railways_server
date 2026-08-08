plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.kotlin.jpa) apply false
}

group = "com.jeffreyalanwang.dutchrailways.backend"

subprojects {
    tasks.withType<Test> {
        filter.isFailOnNoMatchingTests = false
    }
}
