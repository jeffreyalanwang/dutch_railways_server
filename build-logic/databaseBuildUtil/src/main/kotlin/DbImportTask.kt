import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.gradle.TestcontainersBuildService
import javax.inject.Inject

abstract class DbImportTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @get:Input
    abstract val dbContainer: Property<JdbcDatabaseContainer<*>>

    @get:Input
    val dbName = objectFactory.property<String>()
        .convention(dbContainer.map { it.databaseName })

    init {
        dependsOn(dbContainer.map { it.startTaskName })
        finalizedBy(dbContainer.map { it.stopTaskName })
    }

    @get:ServiceReference
    internal abstract val testcontainersService: Property<TestcontainersBuildService>

}