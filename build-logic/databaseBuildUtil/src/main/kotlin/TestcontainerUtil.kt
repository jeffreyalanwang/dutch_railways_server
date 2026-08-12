import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.provider.Provider
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import org.testcontainers.containers.ContainerState
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C
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

val GenericContainer<*>.startTaskName get() = camelCaseJoin("start", containerName, "container")
val GenericContainer<*>.stopTaskName get() = camelCaseJoin("stop", containerName, "container")

fun camelCaseJoin(vararg parts: String) = parts
    .onEach { part ->
        // For safety, confront us if we provide unexpected capitalization
        check(part.all { char -> !char.isLetter() || char.isLowerCase() })
    }
    .reduce { acc, string ->
        acc + string.replaceFirstChar { it.uppercaseChar() }
    }

inline fun <reified T : Any> PropertyFactory.property() = property(T::class.java)

fun exposeHostPort(mappedFromContainer: ContainerState) = mappedFromContainer.run {
    if (host != "localhost") throw NotImplementedError()
    Testcontainers.exposeHostPorts(firstMappedPort)
}
