import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.plugin.use.PluginDependency

private val PluginDependency.artifactCoordinates get() = "$pluginId:$pluginId.gradle.plugin:$version"
private fun DependencyHandler.plugin(dependencyNotation: Provider<PluginDependency>) = dependencyNotation.map { it.artifactCoordinates }

plugins {
    `kotlin-dsl`
}

group = rootProject.group

dependencies {
    implementation(plugin(libs.plugins.build.testcontainers))
    implementation(libs.bundles.testcontainers.postgresql)
}

gradlePlugin {
    plugins {
        register("import-gpkg-task") {
            description = listOf(
                "Provides a task which imports a gpkg",
                "file into a Postgres database.",
            ).joinToString(" ")
            implementationClass = "ImportGpkgPlugin"
        }
        register("import-sql-task") {
            description = listOf(
                "Provides a task to run SQL init scripts",
                "(potentially with resource files).",
            ).joinToString(" ")
            implementationClass = "ImportSqlPlugin"
        }
    }
}