plugins.apply(rootLibs.plugin("kotlin.jvm"))

group = parent!!.group

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(rootLibs.bundle("test.junit"))
    testRuntimeOnly(rootLibs.library("junit.platform.launcher"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}