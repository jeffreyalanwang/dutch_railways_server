plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jpa)
}

group = "com.jeffreyalanwang.dutchrailways.backend"
description = "Spring Boot server application container"

kotlin {
    compilerOptions {
        freeCompilerArgs.add(
            "-Xjsr305=strict",
        )
    }
}

dependencies {
    // TODO Factor out the [dataSource] module
    implementation(project(":routeQuery"))
    implementation(project(":schema"))

    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.graphql.java.extended.scalars)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.spring.boot.graphql.test)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.webtestclient)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.hibernate.spatial)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.data.jpa.test)

    implementation(libs.bundles.hibernate.search)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.springmockk)
}

tasks.test {
    useJUnitPlatform()

    jvmArgs("-Dspring.profiles.active=test")

    // Formally load mockk agent, to appease JVM >=21
    classpath
        .find { "byte-buddy-agent" in it.name }
        ?.let { jvmArgs("-javaagent:${it.absolutePath}") }

    // Required for springmockk
    jvmArgs("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
}