import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.jetbrains.kotlin.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.gradle.DatabaseType
import org.testcontainers.gradle.StartContainersTask
import org.testcontainers.gradle.getContainer
import org.testcontainers.gradle.spec.JdbcContainerSpec
import java.time.OffsetDateTime
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private fun Provider<Directory>.file(path: String) = map { it.file(path) } // mimic [FileProperty.dir]
private fun Provider<Directory>.dir(path: String) = map { it.dir(path) } // mimic [DirectoryProperty.dir]
private fun JdbcContainerSpec.image(spec: Provider<String>) = image(spec.get())
private infix fun String.imageTag(other: Provider<String>) = other.map { "$this:$it" }
private val Provider<SourceSet>.otherSrcDir get() = map { it.otherSrcDir }
private val Provider<out Task>.singleOutputFile get() = map { it.outputs.files.singleFile }.let { layout.file(it) }
private val Provider<out Task>.singleOutputDir get() = map { it.outputs.files.singleFile }.let { layout.dir(it) }
private fun Directory.clearAndMkdirs() = asFile.run { if (exists()) delete(); mkdirs() }
private val Provider<Directory>.sortedFileList get() = map { directory ->
    val childrenAsRelativePaths = directory.asFileTree.map { it.relativeTo(directory.asFile).path }
    childrenAsRelativePaths.sorted().map { relativeChildPath ->
        directory.file(relativeChildPath)
    }
}
private object ExtrasDelegate : ReadWriteProperty<SourceSet, Directory> {
    override fun getValue(thisRef: SourceSet, property: KProperty<*>) =
        thisRef.extra[property.name] as Directory
    override fun setValue(thisRef: SourceSet, property: KProperty<*>, value: Directory) {
        thisRef.extra[property.name] = value
    }
}

plugins {
    `java-test-fixtures`
    id("python-uv-project")
    id("import-gpkg-task")
    id("import-sql-task")

    alias(libs.plugins.build.local.properties)
    alias(libs.plugins.build.testcontainers)
    alias(libs.plugins.build.download)
}

group = "com.jeffreyalanwang.dutchrailways.backend.database"

val localProperties = ext.properties
private var SourceSet.otherSrcDir by ExtrasDelegate

sourceSets {
    create("sql_scripts") {
        otherSrcDir = layout.projectDirectory.dir("src").dir("sql_scripts")
    }
}

uvProject {
    uvProjectDir = layout.projectDirectory.dir("src").dir("scraping")
}

dependencies {
    testcontainersClasspath(libs.bundles.testcontainers.postgresql)
}

testcontainers {
    jdbcContainer("postgres", DatabaseType.POSTGRESQL) {
        image("postgres" imageTag libs.versions.postgres.docker)
        username("postgres")
        password("postgres")
        databaseName("dutch_railways")
    }
}
val postgresContainer = testcontainers.getContainer< JdbcDatabaseContainer<*>>("postgres")

afterEvaluate {
    tasks.named<StartContainersTask>("startPostgresContainer") {
        doNotTrackState(
            listOf(
                "This container is only depended upon when",
                "depending tasks require a running instance.",
            ).joinToString(" ")
        )
    }
}

val scrapeDataTask = tasks.register<UvRunTask>("scrapeData") {
    description = "Scrape data from online APIs to CSV format."

    val notebookInputFile = uvProjectDir.file("ns.ipynb")
    val outDir = layout.buildDirectory.dir("scraping")
    val outNotebook = outDir.file("ns.ipynb")
    val outNsDir = outDir.dir("ns_results")

    onlyIf("Reduce reruns during development; remove this line for up-to-date production data") { false }
    inputs.property("lastRunDate", OffsetDateTime.now().toLocalDate())
    inputs.file(notebookInputFile)
    outputs.dir(outNsDir)

    doFirst {
        // If we do not clear the output directory, the notebook is written
        // to try and reload the .pkl file generated from the last run
        outDir.get().clearAndMkdirs()
    }

    environment(localProperties)
    workingDir(uvProjectDir) // allows discovery of the nsapi package
    args(
        "papermill",
            notebookInputFile.get().asFile.absolutePath,
            outNotebook.get().asFile.absolutePath,
            "-p", "working_dir", outDir.get().asFile.absolutePath, // control output location
    )
}

val downloadGpkgTask = tasks.register<Download>("downloadGpkg") {
    src("https://service.pdok.nl/kadaster/brk-bestuurlijke-gebieden/atom/downloads/BestuurlijkeGebieden_2026.gpkg")
    dest(layout.buildDirectory.file("BestuurlijkeGebieden_2026.gpkg"))
    finalizedBy("verifyGpkg")
    overwrite(false) // allow caching
}
tasks.register<Verify>("verifyGpkg") {
    src(downloadGpkgTask.map { it.outputFiles.single() })
    algorithm(SHA_256)
    checksum("1EFA5BBED78BB5AA9D918D48BCABCD9A3C0E816671545E42CD68BE057B8423E6")
}

tasks.register<ImportGpkgTask>("importGpkg") {
    dbContainer = postgresContainer
    gpkgFile = downloadGpkgTask.map { it.outputFiles.single() }

    commonArgs(
        "-nlt", "PROMOTE_TO_MULTI",
        "-lco", "GEOMETRY_NAME=geom",
        "-lco", "FID=gid",
    )

    importLayers(
        listOf(
            "landgebied",
            "provinciegebied",
            "gemeentegebied",
        ).map { layerName ->
            layerName to "src_$layerName"
        }
    )
}

tasks.register<ImportSqlTask>("importSqlScripts") {
    dependsOn("importGpkg")

    dbContainer = postgresContainer
    initScripts = sourceSets.named("sql_scripts").otherSrcDir
        .dir("init")
        .sortedFileList
    resource(scrapeDataTask.singleOutputDir, "/ns_data") // sql scripts hardcode filenames under `/ns_data`
}

tasks.register<PostgresDumpTask>("buildDatabaseDump") {
    dependsOn("importGpkg")
    dependsOn("importSqlScripts")

    dbContainer = postgresContainer
    sqlDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.sql")
    pgDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.dump")
}

tasks.register("buildDockerVolume") {
    dependsOn("buildDatabase")
    doLast { TODO() }
}
