private val PluginDependency.artifactCoordinates get() = "$pluginId:$pluginId.gradle.plugin:$version"
private fun DependencyHandler.plugin(dependencyNotation: Provider<PluginDependency>) = dependencyNotation.map { it.artifactCoordinates }

plugins {
    `kotlin-dsl`
}

group = rootProject.group

dependencies {
    implementation(plugin(libs.plugins.python.uv))
}

gradlePlugin {
    plugins {
        register("python-uv-subproject") {
            description = listOf(
                "Extensions to com.pswidersk.python-uv-plugin",
                "for working with a nested uv project.",
            ).joinToString(" ")
            implementationClass = "UvProjectPlugin"
        }
    }
}