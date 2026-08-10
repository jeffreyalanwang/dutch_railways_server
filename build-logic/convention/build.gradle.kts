private val PluginDependency.artifactCoordinates get() = "$pluginId:$pluginId.gradle.plugin:$version"
private fun DependencyHandler.plugin(dependencyNotation: Provider<PluginDependency>) = dependencyNotation.map { it.artifactCoordinates }

plugins {
    `kotlin-dsl`
}

group = rootProject.group

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.spring.boot))
}

gradlePlugin {
    plugins {
        named("common-compatibility-conventions") {
            description = listOf(
                "Convention plugin for a module that may be",
                "imported by both frontend and backend code.",
            ).joinToString(" ")
        }
    }
}