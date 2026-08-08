plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jpa)
    id("backend-conventions")
    id("spring-conventions")
}

description = "Spring Boot server application container"

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
}
