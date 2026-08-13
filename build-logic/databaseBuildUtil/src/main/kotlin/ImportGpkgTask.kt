import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import javax.inject.Inject

abstract class ImportGpkgTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DbImportTask(objectFactory) {

    @get:Input internal abstract val gdalContainer: Property<GenericContainer<*>>

    @get:InputFile abstract val gpkgFile: RegularFileProperty
    private val mountableGpkgFile = gpkgFile.asFile.asTestContainerMountable()
    private val gpkgFileContainerPath = "/source.gpkg"

    @get:Input abstract val importLayers: ListProperty<Pair<String, String>>
    @get:Input abstract val commonArgs: ListProperty<String>
    fun importLayers(vararg names: Pair<String, String>) = importLayers.addAll(*names)
    fun importLayers(names: Iterable<Pair<String, String>>) = importLayers.addAll(names)
    fun commonArgs(vararg args: String) = commonArgs.addAll(*args)
    fun commonArgs(args: Iterable<String>) = commonArgs.addAll(args)

    @TaskAction
    fun importGpkg() = gdalContainer.get()
        .apply { check(!isRunning) }
        .also { exposeHostPort(dbContainer.get()) }
        .also { checkIsGpkg(gpkgFile.get()) }
        .withCopyFileToContainer(mountableGpkgFile.get(), gpkgFileContainerPath) // Caution: this could result in a lot of data transfer with a remote Testcontainer runtime
        .withStartupCheckStrategy(OneShotStartupCheckStrategy()) // The GDAL container is executed once per command
        .run {
            commands.get().forEach { command ->
                withCommand(*command).start()
            }
        }

    private val dbConnectionString = dbContainer.zip(dbName) { container, dbName ->
        // Example with username and password: "PG:dbname=dutch_railways user=postgres password=****"

        val prefix = "PG"

        val args1 = mapOf("dbname" to dbName)

        val args2 = container.run {
            mapOf(
                "host" to if (host == "localhost") "host.testcontainers.internal" else throw NotImplementedError(),
                "port" to firstMappedPort.toString(),
                "user" to username,
                "password" to password,
            )
        }

        val argsAll = (args1 + args2).map { (k, v) -> "$k=$v" }.joinToString(" ")

        "$prefix:$argsAll"
    }

    private val commands = importLayers.zip(dbConnectionString) { layers, dbConnectionString ->
        layers.map { (srcLayerName, dbLayerName) ->
            arrayOf(
                "ogr2ogr",
                dbConnectionString,
                gpkgFileContainerPath,
                srcLayerName,
                "-nln", dbLayerName,
            )
        }
    }
}

private fun checkIsGpkg(gpkg: RegularFile) = check(gpkg.asFile.extension == "gpkg")
