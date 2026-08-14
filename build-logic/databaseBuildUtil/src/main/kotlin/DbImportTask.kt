import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.gradle.StartContainersTask
import org.testcontainers.gradle.TestcontainersBuildService
import org.testcontainers.gradle.TestcontainersExtension
import org.testcontainers.gradle.getContainer
import javax.inject.Inject

abstract class DbImportTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @get:Input      abstract val dbContainerName: Property<String>
    @get:Internal   abstract val dbContainer: Property<JdbcDatabaseContainer<*>>
    init { inputs.property("dbContainer", dbContainer.map { it.username + it.dockerImageName }) }

    @get:InputFile  internal abstract val dbContainerMarkerFile: RegularFileProperty // links start task as dependency
    init { finalizedBy(dbContainerName.map { stopTaskName(it) }) }
    fun Project.dbContainer(name: String) {
        dbContainerName.set( name )
        dbContainer.set( testcontainers.getContainer<JdbcDatabaseContainer<*>>(name) )
        dbContainerMarkerFile.set( tasks.startTaskForContainer(name).flatMap { it.markerFile } )
    }

    @get:Input
    val dbName = objectFactory.property<String>()
        .convention(dbContainer.map { it.databaseName })

    @get:ServiceReference
    internal abstract val testcontainersService: Property<TestcontainersBuildService>
}