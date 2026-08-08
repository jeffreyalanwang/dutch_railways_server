plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jpa)
}

group = parent!!.group
description = "Spring Boot server application container"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(project(":routeQuery"))
    implementation(project(":schema"))

    implementation(libs.jackson.kotlin)
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
    implementation(libs.geolatte.geom)
    implementation(libs.bundles.proj4j)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.data.jpa.test)

    implementation(libs.bundles.hibernate.search)

    testImplementation(libs.bundles.test.junit5)
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