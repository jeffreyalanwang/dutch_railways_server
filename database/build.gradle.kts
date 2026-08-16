import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256
import org.testcontainers.gradle.DatabaseType
import org.testcontainers.gradle.spec.JdbcContainerSpec
import java.time.OffsetDateTime
import kotlin.collections.joinToString
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private fun Provider<Directory>.file(path: String) = map { it.file(path) } // mimic [FileProperty.dir]
private fun Provider<Directory>.dir(path: String) = map { it.dir(path) } // mimic [DirectoryProperty.dir]
private fun JdbcContainerSpec.image(spec: Provider<String>) = image(spec.get())
private fun File.existsAndHasChildren() = walk().any { it != this }
private fun Task.onlyIf(vararg onlyIfReason: String, onlyIfSpec: (Task) -> Boolean) = onlyIf(onlyIfReason.joinToString(" ") { it.trim() }, onlyIfSpec)
private val Provider<out Task>.singleOutputDir get() = map { it.outputs.files.singleFile }.let { layout.dir(it) }
private val Directory.sortedFileList get() = asFileTree
    .map { childFile -> childFile.relativeTo(this.asFile).path }
    .sorted()
    .map { relativeChildPath -> this.file(relativeChildPath) }
private class ExtrasDelegate<T : ExtensionAware, V> : ReadWriteProperty<T, V> {
    @Suppress("UNCHECKED_CAST") override fun getValue(thisRef: T, property: KProperty<*>) = thisRef.extra[property.name] as V
    override fun setValue(thisRef: T, property: KProperty<*>, value: V) { thisRef.extra[property.name] = value }
}

plugins {
    alias(libs.plugins.kotlin.jvm)

    `java-test-fixtures`
    id("python-uv-project")
    id("import-gpkg-task")
    id("import-sql-task")
    id("gzip-task")

    alias(libs.plugins.build.local.properties)
    alias(libs.plugins.build.testcontainers)
    alias(libs.plugins.build.download)
}

group = "com.jeffreyalanwang.dutchrailways.backend.database"

val localProperties = ext.properties
private var SourceSet.otherSrcDir by ExtrasDelegate<_, Directory>()

sourceSets.create("sql_scripts") {
    otherSrcDir = layout.projectDirectory.dir("src").dir("sql_scripts")
}

uvProject {
    uvProjectDir = layout.projectDirectory.dir("src").dir("scraping")
}

dependencies {
    testFixturesImplementation(libs.spring.boot.test)
    testFixturesImplementation(libs.spring.boot.data.jpa.test)
    testFixturesImplementation(libs.spring.boot.testcontainers)
    testFixturesApi(libs.bundles.testcontainers.postgresql)
    testcontainersClasspath(libs.bundles.testcontainers.postgresql)
}

val generateCatalogConstants = tasks.register("generateCatalogAccessors") {
    description = "Generate required version catalog accessor for test fixture source compilation."
    val sourceSet = sourceSets.testFixtures
    val outputDir = layout.buildDirectory.dir("generated/source/catalog/${sourceSet.name}/kotlin")
    val fileName = "InjectLibrariesForLibs.kt"
    val value = libs.versions.docker.postgres.build
    val valuePath = listOf("libs", "versions", "docker", "postgres", "build")

    outputs.dir(outputDir)

    doLast {
        val output = valuePath
            .run { dropLast(1) + """const val ${last()} = "${value.get()}"""" }
            .reduceRight { valuePathElement, child ->
                val child = child.prependIndent()
                "object $valuePathElement {\n$child\n}"
            }

        outputDir.get()
            .apply { asFile.mkdirs() }
            .file(fileName)
            .asFile.writeText(output)
    }
}

kotlin.sourceSets.testFixtures {
    kotlin.srcDir(generateCatalogConstants)
}

val postgresContainerName = "postgres"
testcontainers {
    jdbcContainer(postgresContainerName, DatabaseType.POSTGRESQL) {
        image(libs.versions.docker.postgres.build)
        username("postgres")
        password("postgres")
        databaseName("dutch_railways")
    }
}

val scrapeDataTask = tasks.register<UvRunTask>("scrapeData") {
    description = "Scrape data from online APIs to CSV format."

    val notebookInputFile = uvProjectDir.file("ns.ipynb")
    val outDir = layout.buildDirectory.dir("scraping")
    val outNotebook = outDir.file("ns.ipynb")
    val outNsDir = outDir.dir("ns_results")

    gradle.startParameter.run {
        val scrapingDirExists = outDir.map { it.asFile.existsAndHasChildren() }
        val wasExplicitlyRequested = taskNames.any { it == name || it == path || it.endsWith(":$name") }
        val isRerun = systemPropertiesArgs.containsKey("rerun")
        val isRerunTasks = isRerunTasks

        onlyIf(
            "Reduce reruns during development; for up-to-date production data," ,
            "pass --rerun and specify this task explicitly."                    ,
        ) {
            !scrapingDirExists.get() || (wasExplicitlyRequested && isRerun) || isRerunTasks
        }
    }
    inputs.property("lastRunDate", OffsetDateTime.now().toLocalDate())
    inputs.file(notebookInputFile)
    outputs.dir(outNsDir)

    doFirst {
        // If we do not clear the output directory, the notebook tries
        // to reload the .pkl files generated from the last run
        outDir.get().asFile.run {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    environment("TQDM_MININTERVAL" to 10, "TQDM_MAXINTERVAL" to 60)
    environment(localProperties.mapValues { (k, v) -> v.toString() })
    workingDir(uvProjectDir) // allows discovery of the nsapi package
    args(
        "papermill",
            notebookInputFile.get().asFile.absolutePath,
            outNotebook.get().asFile.absolutePath,
            "-p", "working_dir", outDir.get().asFile.absolutePath, // control output location
            "--log-output",
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

afterEvaluate {
    tasks.startTaskForContainer(postgresContainerName) {
        trackedFiles.from(scrapeDataTask.map { it.outputs.files })
        trackedFiles.from(downloadGpkgTask.map { it.outputFiles })
        trackedFiles.from(sourceSets.named("sql_scripts").map { it.otherSrcDir })
    }
}

val importGpkgTask = tasks.register<ImportGpkgTask>("importGpkg") {
    description = listOf(
        "Import Area dataset to temporary tables in",
        "the database on the temporary Docker container",
    ).joinToString(" ")

    dbContainer(postgresContainerName)
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

    dbContainer(postgresContainerName)
    initScripts = sourceSets.named("sql_scripts").map {
        it.otherSrcDir.dir("init").sortedFileList
    }
    resource(scrapeDataTask.singleOutputDir, "/ns_data") // sql scripts hardcode filenames under `/ns_data`
}

val exportDatabaseDumpTask = tasks.register<PostgresDumpTask>("exportDatabaseDump") {
    description = "Export the database on the temporary Docker container"
    dependsOn(importGpkgTask, importSqlScriptsTask)

    dbContainer(postgresContainerName)
    sqlDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.sql")
//    // Binary dump is not useful for initializing a Postgres Docker container
//    pgDumpOutputFile = layout.buildDirectory.dir("dumps").file("postgres.dump")
}

val gzipDatabaseDumpTask = tasks.register<GzipTask>("gzipDatabaseDump") {
    description = listOf(
        "Gzip the built database dump file to",
        "create a final packaged init script."
    ).joinToString(" ")

    inputFile = exportDatabaseDumpTask.flatMap { it.sqlDumpOutputFile }
}

tasks.register<Zip>("zipInitDbDir") {
    description = listOf(
        "Create zip archive which unzips into",
        "root directory of a Postgres Docker",
        "container to initialize the database",
    ).joinToString(" ")
    into("docker-entrypoint-initdb.d") {
        from(gzipDatabaseDumpTask.map { it.outputFile })
        from(sourceSets.named("sql_scripts").map { it.otherSrcDir.file("7_cron.sql") })
    }
}