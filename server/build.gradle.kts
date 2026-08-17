import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("backend-conventions")
    id("spring-conventions")
    alias(libs.plugins.kotlin.jpa)
}

description = "Spring Boot server application container"
version = "0.0.1"

tasks {
    compileTestKotlin {
        compilerOptions {
            // Allow JUnit [ArgumentsProvider]s to read parameter names
            freeCompilerArgs.add("-java-parameters")
        }
    }

    register<BootBuildImage>("publishBootImage") {
        description = "Build and publish a Docker image."

        dependsOn(check)

        // defaults to docker.io using local Docker credentials
        publish = true
        imageName = "jeffreyalanwang/dutch-railways-server"
        tags.add(imageName.map { "$it:$version" })

        archiveFile = bootJar.flatMap { it.archiveFile }
    }
}

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
    testImplementation(testFixtures(project(":database")))
    testImplementation(testFixtures(project(":geometryUtil")))

    implementation(libs.bundles.hibernate.search)
}
