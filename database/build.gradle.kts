import java.time.OffsetDateTime
private fun Provider<Directory>.file(path: String) = map { it.file(path) } // mimic [FileProperty.dir]
private fun Provider<Directory>.dir(path: String) = map { it.dir(path) } // mimic [DirectoryProperty.dir]

//buildscript {
//    dependencies {
//        classpath("org.testcontainers:testcontainers:2.0.5")
//        classpath("org.testcontainers:postgresql:1.21.4")
//    }
//}

plugins {
    `java-test-fixtures`
    id("python-uv-project")

    alias(libs.plugins.local.properties)
}

group = "com.jeffreyalanwang.dutchrailways.backend.database"

val scrapingSrcDir = layout.projectDirectory.dir("src").dir("scraping")
val scrapingOutDir = layout.buildDirectory.dir("scraping")
val localProperties = ext.properties

uvProject {
    uvProjectDir = scrapingSrcDir
}

tasks.register<UvRunTask>("scrapeData") {
    description = "Scrape data from online APIs to CSV format."

    inputs.property("lastRunDate", OffsetDateTime.now().toLocalDate())
    inputs.file(scrapingOutDir.file("ns.ipynb"))
    outputs.dir(scrapingOutDir.dir("ns_results"))

    doFirst {
        // If we do not clear the output directory, the notebook is written
        // to try and reload the .pkl file generated from the last run
        delete(scrapingOutDir.dir("ns_results"))
    }

    environment(localProperties)
    args(
        "--directory", scrapingSrcDir, // allows discovery of the nsapi package
        "papermill",
            scrapingSrcDir.file("ns.ipynb"),
            scrapingOutDir.map { it.file("ns.ipynb") }.get(),
            "-p", "working_dir", scrapingOutDir.get(), // control output location
    )
}

tasks.register("buildDatabase") {
    inputs.dir(scrapingOutDir)
    doLast { TODO() }
}

tasks.register("buildDockerVolume") {
    dependsOn("buildDatabase")
    doLast { TODO() }
}

tasks.register("generateTestFixtures") {
    dependsOn("scrapeData")
    dependsOn("buildDatabase")
    doLast { TODO() }
}
