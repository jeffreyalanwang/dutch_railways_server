plugins {
    id("backend-conventions")
}

description = "Implementation of transit route planning algorithms"

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(libs.kotlinx.datetime)
}
