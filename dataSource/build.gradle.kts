import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("shared")
    id("org.jooq.jooq-codegen-gradle") version "3.21.6"
}

dependencies {
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    implementation("org.jooq:jooq:3.21.5")
    jooqCodegen("org.jooq:jooq-postgres-extensions:3.21.6")
}

jooq {
    configuration {
        jdbc {
            val localProperties = rootProject
                .file("local.properties")
                .absolutePath
                .let { loadProperties(it) }

            driver = "org.postgresql.Driver"
            url = (localProperties["db.databaseName"] as String)
                .let { "jdbc:postgresql://localhost:5432/${it}" }
            user = localProperties["db.username"] as String
            password = localProperties["db.password"] as String
        }

        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                includes = """public\..*"""
                excludes = """_.*"""
                isIncludeRoutines = false
            }
            target {
                packageName = "com.jeffeyalanwang.dutchrailways.backend.dataSource.generated"
                directory = "src/main/kotlin"
                locale = "en_US"
                isClean = true
            }
        }
    }
}