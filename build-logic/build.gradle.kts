plugins {
    `kotlin-dsl`
}

group = "com.jeffreyalanwang.dutchrailways.backend.build-logic"

dependencies {
    implementation(libs.plugins.kotlin.jvm)
}

private fun DependencyHandler.implementation(dependencyNotation: Provider<PluginDependency>) =
    implementation(
        dependencyNotation.get().run {
            "$pluginId:$pluginId.gradle.plugin:$version"
        }
    )