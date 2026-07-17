plugins {
    id("shared")
    id("org.springframework.boot") version "4.1.0"
}

dependencies {
    // TODO Factor out the [api] and [dataSource] modules
    implementation("jakarta.persistence:jakarta.persistence-api:4.0.0-M6")
    implementation("org.springframework.data:spring-data-jpa:4.1.0")
    implementation("org.hibernate.orm:hibernate-spatial:7.4.5.Final")
    implementation(project(":routeQuery"))
    runtimeOnly("org.postgresql:postgresql:42.7.13")

    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.graphql-java:graphql-java-extended-scalars:24.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
