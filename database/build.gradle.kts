import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.gradle.kotlin.dsl.testcontainersClasspath
import org.jetbrains.kotlin.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.gradle.DatabaseType
import org.testcontainers.gradle.getContainer
import org.testcontainers.gradle.spec.JdbcContainerSpec
import org.testcontainers.utility.MountableFile
import java.time.OffsetDateTime

private fun Provider<Directory>.file(path: String) = map { it.file(path) } // mimic [FileProperty.dir]
private fun Provider<Directory>.dir(path: String) = map { it.dir(path) } // mimic [DirectoryProperty.dir]
private fun JdbcContainerSpec.image(spec: Provider<String>) = image(spec.get())
private infix fun String.imageTag(other: Provider<String>) = other.map { "$this:$it" }
private fun FileSystemLocation.asTestContainerMountable() = MountableFile.forHostPath(this.asFile.path)
private fun Provider<out FileSystemLocation>.asTestContainerMountable() = map { it.asTestContainerMountable() }
private val Provider<SourceSet>.customDirectory get() = map { it.customDirectory }

private var SourceSet.customDirectory
    get() = extra["dir"] as Directory
    set(it) { extra["dir"] = it }

plugins {
    `java-test-fixtures`
    id("python-uv-project")

    alias(libs.plugins.local.properties)

    id("io.github.regulskimichal.testcontainers") version "0.1.1" // TODO: factor out
    id("de.undercouch.download") version "5.6.0"
}

group = "com.jeffreyalanwang.dutchrailways.backend.database"

val postgresDbName = "dutch_railways"
val scrapingOutDir = layout.buildDirectory.dir("scraping")
val areaShapefile = layout.buildDirectory.file("BestuurlijkeGebieden_2026.gpkg")
val localProperties = ext.properties

sourceSets {
    create("sql_scripts") {
        customDirectory = layout.projectDirectory.dir("src").dir("sql_scripts")
    }
}

uvProject {
    uvProjectDir = layout.projectDirectory.dir("src").dir("scraping")
}

tasks.register<UvRunTask>("scrapeData") {
    description = "Scrape data from online APIs to CSV format."

    inputs.property("lastRunDate", OffsetDateTime.now().toLocalDate())
    inputs.file(scrapingOutDir.file("ns.ipynb"))
    outputs.dir(scrapingOutDir.dir("ns_results"))

    doFirst {
        // If we do not clear the output directory, the notebook is written
        // to try and reload the .pkl file generated from the last run
        delete(scrapingOutDir)
    }

    environment(localProperties)
    workingDir(uvProjectDir) // allows discovery of the nsapi package
    args(
        "papermill",
            uvProjectDir.file("ns.ipynb"),
            scrapingOutDir.map { it.file("ns.ipynb") }.get(),
            "-p", "working_dir", scrapingOutDir.get(), // control output location
    )
}

dependencies {
    testcontainersClasspath("org.testcontainers:testcontainers-postgresql:2.0.5") // TODO to version catalog
}

testcontainers {
    jdbcContainer("postgres", DatabaseType.POSTGRESQL) {
        image("postgres" imageTag libs.versions.postgres.docker)
        databaseName(postgresDbName)
    }
    genericContainer("gdal") {
        image("ghcr.io/osgeo/gdal:ubuntu-small-latest")
    }
}

tasks.register<Download>("downloadShapefile") {
    src("https://service.pdok.nl/kadaster/brk-bestuurlijke-gebieden/atom/downloads/BestuurlijkeGebieden_2026.gpkg")
    dest(areaShapefile)
    finalizedBy("verifyShapefile")
}

tasks.register<Verify>("verifyShapefile") {
    src(areaShapefile)
    algorithm(SHA_256)
    checksum("1EFA5BBED78BB5AA9D918D48BCABCD9A3C0E816671545E42CD68BE057B8423E6")
}

tasks.register("importShapefile") {
    val container = testcontainers.getContainer<GenericContainer<*>>("gdal")
    val areaShapefileContainer = areaShapefile.map { "/" + it.asFile.name }

    dependsOn("startPostgresContainer")
    dependsOn("startGdalContainer")
    usesService(testcontainers.service)
    finalizedBy("stopPostgresContainer")
    finalizedBy("stopGdalContainer")

    inputs.file(areaShapefile)

    fun buildLayerImportCommand(srcLayerName: String, importedLayerName: String) = arrayOf(
        "ogr2ogr",
            "PG:dbname=$postgresDbName", // with username and password: "PG:dbname=dutch_railways user=postgres password=****"
                areaShapefileContainer.get(),
                srcLayerName,
                "-nln", importedLayerName,
            "-nlt", "PROMOTE_TO_MULTI",
            "-lco", "GEOMETRY_NAME=geom",
            "-lco", "FID=gid",
    )

    val mountableAreaShapefile = areaShapefile.asTestContainerMountable()
    doLast {
        container.get().run {
            copyFileToContainer(mountableAreaShapefile.get(), areaShapefileContainer.get())
            listOf("landgebied", "provinciegebied", "gemeentegebied")
                .map { layerName -> layerName to "src_$layerName" }
                .map { (srcLayerName, dbLayerName) ->
                    buildLayerImportCommand(srcLayerName = srcLayerName, importedLayerName = dbLayerName) }
                .forEach { args ->
                    execInContainer(*args)
                }
        }
    }
}

tasks.register("buildDatabase") {
    val container = testcontainers.getContainer<JdbcDatabaseContainer<*>>("postgres")
    val initScripts = sourceSets.named("sql_scripts").customDirectory.dir("init")
    val initScriptsContainer = "/sql"

    dependsOn("startPostgresContainer")
    usesService(testcontainers.service)
    finalizedBy("stopPostgresContainer")

    inputs.dir(scrapingOutDir.dir("ns_results"))
    inputs.files(initScripts)

    val postgresDbName = postgresDbName
    val mountableInitScripts = initScripts.asTestContainerMountable()
    val mountableNsData = scrapingOutDir.dir("ns_results").asTestContainerMountable()
    doLast {
        container.get().run {
            copyFileToContainer(mountableInitScripts.get(), initScriptsContainer)
            copyFileToContainer(mountableNsData.get(), "/ns_data") // sql scripts hardcode filenames under `/ns_data`
            execInContainer(
                "sh", "-c",
                "cat $initScriptsContainer/* | psql -d $postgresDbName",
            )
        }
    }
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
