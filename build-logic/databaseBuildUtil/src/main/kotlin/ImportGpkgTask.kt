import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.testcontainers.Testcontainers
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.gradle.TestcontainersBuildService
import javax.inject.Inject

abstract class ImportGpkgTask @Inject constructor(
    propertyFactory: PropertyFactory,
) : DefaultTask() {

    @get:Input internal abstract val gdalContainer: Property<GenericContainer<*>>
    @get:Input abstract val dbContainer: Property<JdbcDatabaseContainer<*>>
    @get:Input val dbName = propertyFactory.property<String>().convention(dbContainer.map { it.databaseName })

    @get:Input abstract val gpkgFile: RegularFileProperty
    private val mountableGpkg = gpkgFile.asFile.asTestContainerMountable()
    private val gpkgContainerPath = "/source.gpkg"

    @get:Input abstract val importLayers: ListProperty<Pair<String, String>>
    @get:Input abstract val commonArgs: ListProperty<String>
    fun importLayers(vararg names: Pair<String, String>) = importLayers.addAll(*names)
    fun importLayers(names: Iterable<Pair<String, String>>) = importLayers.addAll(names)
    fun commonArgs(vararg args: String) = commonArgs.addAll(*args)
    fun commonArgs(args: Iterable<String>) = commonArgs.addAll(args)

    @get:ServiceReference internal abstract val testcontainersService: Property<TestcontainersBuildService>

    init {
        dependsOn(dbContainer.map { it.startTaskName })
        finalizedBy(dbContainer.map { it.stopTaskName })
    }

    @TaskAction
    fun importGpkg() = gdalContainer.get()
        .apply { check(!isRunning) }
        .also { exposeHostPort(dbContainer.get()) }
        .also { checkIsGpkg(gpkgFile.get()) }
        .withCopyFileToContainer(mountableGpkg.get(), gpkgContainerPath)
        .withStartupCheckStrategy(OneShotStartupCheckStrategy())
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
                gpkgContainerPath,
                srcLayerName,
                "-nln", dbLayerName,
            )
        }
    }
}

private fun checkIsGpkg(gpkg: RegularFile) = check(gpkg.asFile.extension == "gpkg")
private fun exposeHostPort(mappedFromContainer: ContainerState) = mappedFromContainer.run {
    if (host != "localhost") throw NotImplementedError()
    Testcontainers.exposeHostPorts(firstMappedPort)
}
