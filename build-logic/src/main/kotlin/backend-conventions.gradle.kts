plugins.apply(rootLibs.plugin("kotlin.jvm"))

group = parent!!.group

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(rootLibs.bundle("test.junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}