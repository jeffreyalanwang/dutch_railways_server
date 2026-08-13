import org.gradle.api.provider.Provider
import org.testcontainers.Testcontainers
import org.testcontainers.containers.ContainerState
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

fun startTaskName(containerName: String) = camelCaseJoin("start", containerName, "container")
fun stopTaskName(containerName: String) = camelCaseJoin("stop", containerName, "container")

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
