private val PluginDependency.artifactCoordinates get() = "$pluginId:$pluginId.gradle.plugin:$version"
private fun DependencyHandler.implementation(dependencyNotation: Provider<PluginDependency>) =
    implementation( dependencyNotation.map { it.artifactCoordinates } )

plugins {
    `kotlin-dsl`
}

group = "com.jeffreyalanwang.dutchrailways.backend.build-logic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.plugins.kotlin.jvm)
}
