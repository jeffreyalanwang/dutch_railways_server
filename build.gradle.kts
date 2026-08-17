plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.kotlin.jpa) apply false
}

group = "com.jeffreyalanwang.dutchrailways.backend"
version = "0.0.1"

subprojects {
    tasks.withType<Test> {
        filter.isFailOnNoMatchingTests = false
    }
}

tasks.register<Exec>("publishDockerCompose") {
    description = "Publish to Docker Compose"
    commandLine(
        "docker", "compose", "publish",
        "jeffreyalanwang/dutch-railways:$version",
    )
}

tasks.register<Exec>("fetchAndRunDockerCompose") {
    description = "Run Docker Compose from registry"
    commandLine(
        "docker", "compose",
        "-f",
            "oci://docker.io/jeffreyalanwang/dutch-railways:$version",
        "up",
    )
}
