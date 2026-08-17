import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class PostgresDumpTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DbImportTask(objectFactory) {

    @get:Optional @get:OutputFile
    abstract val sqlDumpOutputFile: RegularFileProperty

    @get:Optional @get:OutputFile
    abstract val pgDumpOutputFile: RegularFileProperty

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
    fun saveDumps() = dbContainer.get()
        .apply { followStdErrTo(logger) }
        .run {
            val isSqlDumpRequested = sqlDumpOutputFile.isPresent
            val isPgDumpRequested = pgDumpOutputFile.isPresent

            check(isSqlDumpRequested || isPgDumpRequested)
            if (isSqlDumpRequested) {
                val sqlDumpOutputFile = sqlDumpOutputFile.get()
                val sqlDumpPathHost = sqlDumpOutputFile.asFile.absolutePath
                val sqlDumpPathContainer = "/" + sqlDumpOutputFile.asFile.name

                execInContainer(
                    *dumpCommandArgs.get().plus(
                        "-f", sqlDumpPathContainer
                    )
                )
                copyFileFromContainer(sqlDumpPathContainer, sqlDumpPathHost)
            }
            if (isPgDumpRequested) {
                val pgDumpOutputFile = pgDumpOutputFile.get()
                val pgDumpPathHost = pgDumpOutputFile.asFile.absolutePath
                val pgDumpPathContainer = "/" + pgDumpOutputFile.asFile.name

                execInContainer(
                    *dumpCommandArgs.get().plus(
                        "-F", "c",
                        "-f", pgDumpPathContainer
                    )
                )
                copyFileFromContainer(pgDumpPathContainer, pgDumpPathHost)
            }
        }

}