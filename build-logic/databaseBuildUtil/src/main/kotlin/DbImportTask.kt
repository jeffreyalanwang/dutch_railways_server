import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.gradle.TestcontainersBuildService
import org.testcontainers.gradle.TestcontainersExtension
import org.testcontainers.gradle.getContainer
import javax.inject.Inject

abstract class DbImportTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @get:Input abstract val dbContainerName: Property<String>
    @get:Internal abstract val dbContainer: Property<JdbcDatabaseContainer<*>>
    fun dbContainer(extension: TestcontainersExtension, name: String) {
        dbContainerName = name
        dbContainer = extension.getContainer<JdbcDatabaseContainer<*>>(name)
    }
    @get:Input internal val dbContainerGradleInputKey = dbContainer.map { it.username + it.dockerImageName }

    @get:Input
    val dbName = objectFactory.property<String>()
        .convention(dbContainer.map { it.databaseName })

    @get:ServiceReference
    internal abstract val testcontainersService: Property<TestcontainersBuildService>

    init {
        onlyIf("Start task for testcontainer $dbContainerName was configured to skip execution.") {
            testcontainersService.get().run {
                wasContainerStarted(dbContainerName.get())
            }
        }
        dependsOn(dbContainerName.map { startTaskName(containerName = it) })
        finalizedBy(dbContainerName.map { stopTaskName(containerName = it) })
    }
}