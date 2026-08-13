import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.jetbrains.kotlin.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256
import org.testcontainers.gradle.DatabaseType
import org.testcontainers.gradle.StartContainersTask
import org.testcontainers.gradle.spec.JdbcContainerSpec
import java.time.OffsetDateTime
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private fun Provider<Directory>.file(path: String) = map { it.file(path) } // mimic [FileProperty.dir]
private fun Provider<Directory>.dir(path: String) = map { it.dir(path) } // mimic [DirectoryProperty.dir]
private fun JdbcContainerSpec.image(spec: Provider<String>) = image(spec.get())
private val Provider<SourceSet>.otherSrcDir get() = map { it.otherSrcDir }
private fun Task.dependsOnAndCopiesInputs(vararg tasks: Provider<out Task>) = tasks.forEachIndexed { i, task ->
    dependsOn(task)
    inputs.property(i.toString(), task.map { it.inputs.properties })
    inputs.files(task.map { it.inputs.files })
}
private val Provider<out Task>.singleOutputFile get() = map { it.outputs.files.singleFile }.let { layout.file(it) }
private val Provider<out Task>.singleOutputDir get() = map { it.outputs.files.singleFile }.let { layout.dir(it) }
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
//    id("import-sql-task")

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

val postgresContainerName = "postgres"
testcontainers {
    jdbcContainer(postgresContainerName, DatabaseType.POSTGRESQL) {
        image(libs.versions.postgres.docker)
        username("postgres")
        password("postgres")
        databaseName("dutch_railways")
    }
}

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
        outDir.get().asFile.run {
            if (exists()) delete()
            mkdirs()
        }
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
    description = "Download source dataset for Area database table"

    src("https://service.pdok.nl/kadaster/brk-bestuurlijke-gebieden/atom/downloads/BestuurlijkeGebieden_2026.gpkg")
    dest(layout.buildDirectory.file("BestuurlijkeGebieden_2026.gpkg"))
    finalizedBy("verifyGpkg")
    overwrite(false) // allow caching
}
val verifyGpkgTask = tasks.register<Verify>("verifyGpkg") {
    description = "Verify source dataset for Area database table"

    src(downloadGpkgTask.map { it.outputFiles.single() })
    algorithm(SHA_256)
    checksum("1EFA5BBED78BB5AA9D918D48BCABCD9A3C0E816671545E42CD68BE057B8423E6")
}

val importGpkgTask = tasks.register<ImportGpkgTask>("importGpkg") {
    description = listOf(
        "Import Area dataset to temporary tables in",
        "the database on the temporary Docker container",
    ).joinToString(" ")
    dbContainer(testcontainers, postgresContainerName)
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

val importSqlScriptsTask = tasks.register<ImportSqlTask>("importSqlScripts") {
    description = "Run SQL init scripts on database in the temporary Docker container"
    dependsOn(importGpkgTask)

    dbContainer(testcontainers, postgresContainerName)
    initScripts = sourceSets.named("sql_scripts").otherSrcDir
        .dir("init")
        .sortedFileList
    resource(scrapeDataTask.singleOutputDir, "/ns_data") // sql scripts hardcode filenames under `/ns_data`
}

val exportDatabaseDumpTask = tasks.register<PostgresDumpTask>("exportDatabaseDump") {
    description = "Export the database on the temporary Docker container"
    dependsOnAndCopiesInputs(importGpkgTask, importSqlScriptsTask)

    dbContainer(testcontainers, postgresContainerName)
    sqlDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.sql")
//    // Binary dump is not useful for initializing a Postgres Docker container
//    pgDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.dump")
}

val gzipDatabaseDumpTask = tasks.register("gzipDatabaseDump") {
    description = listOf(
        "Gzip the built database dump file to",
        "create a final packaged init script"
    ).joinToString(" ")

    val inputFile = exportDatabaseDumpTask.map { it.sqlDumpOutputFile }
    val outputFile = inputFile.zip(layout.buildDirectory) { srcFile, buildDir ->
        buildDir.file("${ srcFile.get().asFile.nameWithoutExtension }.sql.gz")
    }

    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        ant.withGroovyBuilder {
            "gzip"("src" to inputFile, "zipfile" to outputFile)
        }
    }
}
