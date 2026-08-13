import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.testcontainers.containers.ExecInContainerPattern.execInContainer
import javax.inject.Inject
import kotlin.collections.plus

abstract class PostgresDumpTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DbImportTask(objectFactory) {

    @get:Optional @get:OutputFile
    abstract val sqlDumpOutputFile: RegularFileProperty
    private val isSqlDumpRequested get() = sqlDumpOutputFile.isPresent
    private val sqlDumpPathHost = sqlDumpOutputFile.map { it.asFile.absolutePath }
    private val sqlDumpPathContainer = sqlDumpOutputFile.map { "/" + it.asFile.name }

    @get:Optional @get:OutputFile
    abstract val pgDumpOutputFile: RegularFileProperty
    private val isPgDumpRequested get() = pgDumpOutputFile.isPresent
    private val pgDumpPathHost = pgDumpOutputFile.map { it.asFile.absolutePath }
    private val pgDumpPathContainer = pgDumpOutputFile.map { "/" + it.asFile.name }

    private val dumpCommandArgs = dbContainer.zip(dbName) { dbContainer, dbName ->
        // Postgres containers do not require a password
        // for connections originating inside the container
        arrayOf(
            "pg_dump",
            "-h", "localhost",
            "-U", dbContainer.username,
            "-d", dbName,
        )
    }

    @TaskAction
    fun saveDumps() = dbContainer.get().run {
        check(isSqlDumpRequested || isPgDumpRequested)
        if (isSqlDumpRequested) {
            execInContainer(
                *dumpCommandArgs.get().plus(
                    "-f", sqlDumpPathContainer.get()
                )
            )
            copyFileFromContainer(sqlDumpPathContainer.get(), sqlDumpPathHost.get())
        }
        if (isPgDumpRequested) {
            execInContainer(
                *dumpCommandArgs.get().plus(
                    "-F", "c",
                    "-f", pgDumpPathContainer.get()
                )
            )
            copyFileFromContainer(pgDumpPathContainer.get(), pgDumpPathHost.get())
        }
    }

}