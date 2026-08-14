import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.testcontainers.Testcontainers
import org.testcontainers.containers.Container
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.ExecConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.gradle.StartContainersTask
import org.testcontainers.gradle.TestcontainersExtension
import org.testcontainers.utility.MountableFile
import java.io.File

/** Convert a file so that it can be passed into [ContainerState.copyFileToContainer]. */
fun File.asTestContainerMountable(): MountableFile = MountableFile.forHostPath(toPath())

/** @see [asTestContainerMountable] */
fun Provider<File>.asTestContainerMountable() = map { it.asTestContainerMountable() }

fun <T, R> Provider<out Iterable<T>>.mapEach(transform: (T) -> R) =
    map { iterable ->
        iterable.map { item ->
            transform(item)
        }
    }

val Project.testcontainers get() = extensions.getByType<TestcontainersExtension>()

fun startTaskName(containerName: String) = camelCaseJoin("start", containerName, "container")
fun stopTaskName(containerName: String) = camelCaseJoin("stop", containerName, "container")
fun TaskContainer.startTaskForContainer(containerName: String) = named<StartContainersTask>(startTaskName(containerName))
fun TaskContainer.startTaskForContainer(containerName: String, configurationAction: StartContainersTask.() -> Unit) = named<StartContainersTask>(startTaskName(containerName), configurationAction)

fun camelCaseJoin(vararg parts: String) = parts
    .onEach { part ->
        // For safety, confront us if we provide unexpected capitalization
        check(part.all { char -> !char.isLetter() || char.isLowerCase() })
    }
    .reduce { acc, string ->
        acc + string.replaceFirstChar { it.uppercaseChar() }
    }

fun exposeHostPort(mappedFromContainer: ContainerState) = mappedFromContainer.run {
    if (host != "localhost") throw NotImplementedError()
    Testcontainers.exposeHostPorts(firstMappedPort)
}

fun <T> Array<T>.plus(vararg elements: T) = plus(elements)

/** Only works with a Postgres database. */
fun JdbcDatabaseContainer<*>.execSqlScript(
    containerPath: String,
    containerUser: String = this.username,
    databaseName: String = this.databaseName,
): Container.ExecResult = execInContainer(
    ExecConfig.builder()
        .user(containerUser)
        .command(arrayOf(
            "sh", "-c",
            "cat $containerPath | psql -d $databaseName"
        ))
    .build()
)

fun Container<*>.followStdErrTo(logger: Logger) = followOutput {
    if (it.type == OutputFrame.OutputType.STDERR)
        logger.error(it.utf8StringWithoutLineEnding)
}

inline fun String.newLines(oldLineCount: Int, handleNewLineCount: (Int) -> Unit) =
    lines()
    .also { handleNewLineCount(it.size) }
    .drop(oldLineCount)
    .joinToString(System.lineSeparator())
