import sun.jvmstat.monitor.MonitoredVmUtil.jvmArgs

plugins {
    id("backend-conventions")
    id("spring-conventions")
    alias(libs.plugins.kotlin.jpa)
}

description = "Spring Boot server application container"

dependencies {
    implementation(project(":geometryUtil"))
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
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.data.jpa.test)
    testImplementation(testFixtures(project(":geometryUtil")))

    implementation(libs.bundles.hibernate.search)
}

tasks.compileTestKotlin {
    compilerOptions {
        // Allow JUnit [ArgumentsProvider]s to read parameter names
        freeCompilerArgs.add("-java-parameters")
    }
}