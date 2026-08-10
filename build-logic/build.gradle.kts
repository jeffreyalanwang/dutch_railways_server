plugins {
    `kotlin-dsl`
}

group = "com.jeffreyalanwang.dutchrailways"

subprojects {
    tasks.register("prepareKotlinBuildScriptModel") {
        description = "Dummy task to prevent IDE sync issues."
    }
}
