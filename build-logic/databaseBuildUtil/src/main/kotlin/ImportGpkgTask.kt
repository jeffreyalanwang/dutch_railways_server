import jdk.internal.vm.ThreadContainers.container
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.shaded.org.bouncycastle.cms.RecipientId.password
import java.lang.System.lineSeparator
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

abstract class ImportGpkgTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DbImportTask(objectFactory) {

    @get:Internal internal abstract val gdalContainer: Property<GenericContainer<*>>
    @get:Input internal var gdalContainerGradleInputKey = gdalContainer.map { it.dockerImageName }

    @get:InputFile abstract val gpkgFile: RegularFileProperty
    private val mountableGpkgFile = gpkgFile.asFile.asTestContainerMountable()
    private val gpkgFileContainerPath = "/source.gpkg"

    @get:Input abstract val importLayers: ListProperty<Pair<String, String>>
    @get:Input abstract val commonArgs: ListProperty<String>
    fun importLayers(vararg names: Pair<String, String>) = importLayers.addAll(*names)
    fun importLayers(names: Iterable<Pair<String, String>>) = importLayers.addAll(names)
    fun commonArgs(vararg args: String) = commonArgs.addAll(*args)
    fun commonArgs(args: Iterable<String>) = commonArgs.addAll(args)

    private val dbConnectionString = dbContainer.zip(dbName) { dbContainer, dbName ->
        ogr2ogrConnectionString(
            hostname = if (dbContainer.host == "localhost") "host.testcontainers.internal" else throw NotImplementedError(),
            port = dbContainer.firstMappedPort,
            databaseName = dbName,
            username = dbContainer.username,
            password = dbContainer.password,
        )
    }

    @TaskAction
    fun importGpkg() = gdalContainer.get()
        .apply { check(!isRunning) }
        .also { exposeHostPort(dbContainer.get()) }
        .also { checkIsGpkg(gpkgFile.get()) }
        .withCopyFileToContainer(mountableGpkgFile.get(), gpkgFileContainerPath) // Caution: this could result in a lot of data transfer with a remote Testcontainer runtime
        .withStartupCheckStrategy(OneShotStartupCheckStrategy()) // The GDAL container is executed once per command
        .run {
            importLayers.get().forEach { (srcLayerName, dbLayerName) ->
                withCommand(*ogr2ogrCommand(
                    dbConnectionString = dbConnectionString.get(),
                    gpkgFileContainerPath = gpkgFileContainerPath,
                    srcLayerName = srcLayerName,
                    dbLayerName = dbLayerName,
                )).run {
                    start()
                    logs.let { logger.lifecycle(it.trim()) }
                    stop()
                }
            }
        }

}

private fun checkIsGpkg(gpkg: RegularFile) = check(gpkg.asFile.extension == "gpkg")

private fun ogr2ogrCommand(
    dbConnectionString: String,
    gpkgFileContainerPath: String,
    srcLayerName: String,
    dbLayerName: String,
) = arrayOf(
    "ogr2ogr",
    "-progress",
    dbConnectionString,
    gpkgFileContainerPath,
    srcLayerName,
    "-nln", dbLayerName,
)

/** Example with username and password: "PG:dbname=dutch_railways user=postgres password=****" */
private fun ogr2ogrConnectionString(
    hostname: String,
    port: Int,
    databaseName: String,
    username: String,
    password: String,
) = "PG:" + listOf(
    "dbname" to databaseName,
    "host" to hostname,
    "port" to port.toString(),
    "user" to username,
    "password" to password,
).joinToString(" ") { (k, v) -> "$k=$v" }
