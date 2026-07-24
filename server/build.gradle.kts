plugins {
    id("shared")
    id("org.springframework.boot") version "4.1.0"
    kotlin("plugin.jpa") version "2.4.10"
    id("com.google.devtools.ksp") version "2.3.10"
}

dependencies {
    // TODO Factor out the [api] and [dataSource] modules
    implementation("org.hibernate.orm:hibernate-spatial:7.4.5.Final")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    implementation(project(":routeQuery"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.graphql-java:graphql-java-extended-scalars:24.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("com.ninja-squad:springmockk:5.0.1")
    testImplementation("io.mockk:mockk-context-parameters:1.14.11")
    testImplementation("org.springframework.boot:spring-boot-webtestclient")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}

tasks.test {
    // Required for springmockk
    jvmArgs = listOf("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
}
