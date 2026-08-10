plugins.apply("backend-conventions")
plugins.apply(rootLibs.plugin("kotlin-spring"))
plugins.apply(rootLibs.plugin("spring-boot"))

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(platform(rootLibs.library("spring-boot-bom")))
    testImplementation(rootLibs.library("springmockk"))
}

tasks.withType<Test> {
    jvmArgs("-Dspring.profiles.active=test")

    // Required for springmockk
    jvmArgs("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")

    // [classpath] must not be read until the hosting
    // `build.gradle.kts` has declared all dependencies
    afterEvaluate {
        // Formally load mockk agent, to appease JVM >=21
        classpath.singleOrNull { "byte-buddy-agent" in it.name }?.let {
            jvmArgs("-javaagent:${it.absolutePath}")
        }
    }
}
