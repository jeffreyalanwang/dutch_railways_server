plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "backend"

include("server")
include("routeQuery")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs.create("libs") {
        version("kotlin", "2.4.10")
        version("spring-boot", "4.1.0")
        version("spring", "7.4.1.Final")

        plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
        plugin("kotlin-spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
        plugin("spring-dependency-management", "io.spring.dependency-management").version("1.1.7")
        plugin("spring-boot", "org.springframework.boot").versionRef("spring-boot")
        plugin("kotlin-jpa", "org.jetbrains.kotlin.plugin.jpa").versionRef("kotlin")

        library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
        library("kotlin-test-junit5", "org.jetbrains.kotlin", "kotlin-test-junit5").versionRef("kotlin")
        library("kotlin-build-tools", "org.jetbrains.kotlin", "kotlin-build-tools-impl").versionRef("kotlin")

        library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").withoutVersion()
        library("kotlinx-coroutines-reactor", "org.jetbrains.kotlinx", "kotlinx-coroutines-reactor").withoutVersion()

        library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
        library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()

        library("hibernate-spatial", "org.hibernate.orm", "hibernate-spatial").versionRef("spring")
        library("spring-boot-starter-data-jpa", "org.springframework.boot", "spring-boot-starter-data-jpa").versionRef("spring-boot")
        library("postgresql", "org.postgresql", "postgresql").version("42.7.13")

        library("spring-boot-starter-graphql", "org.springframework.boot", "spring-boot-starter-graphql").versionRef("spring-boot")
        library("spring-boot-starter-webmvc", "org.springframework.boot", "spring-boot-starter-webmvc").versionRef("spring-boot")
        library("spring-boot-webtestclient", "org.springframework.boot", "spring-boot-webtestclient").versionRef("spring-boot")
        library("spring-boot-data-jpa-test", "org.springframework.boot", "spring-boot-data-jpa-test").versionRef("spring-boot")
        library("spring-boot-graphql-test", "org.springframework.boot", "spring-boot-graphql-test").versionRef("spring-boot")
        library("spring-boot-webmvc-test", "org.springframework.boot", "spring-boot-webmvc-test").versionRef("spring-boot")
        library("springmockk", "com.ninja-squad", "springmockk").version("5.0.1")

        library("graphql-java-extended-scalars", "com.graphql-java", "graphql-java-extended-scalars").version("24.0")
    }
}