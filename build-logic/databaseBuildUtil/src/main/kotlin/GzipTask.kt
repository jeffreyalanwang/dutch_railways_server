import org.gradle.api.AntBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.testcontainers.containers.ExecInContainerPattern.execInContainer
import java.io.File
import javax.inject.Inject

private fun File.resolveSibling(mapName: (String) -> String) = resolveSibling(mapName(name))

abstract class GzipTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    val outputFile = objectFactory.fileProperty()
        .convention {
            inputFile.get().asFile.resolveSibling { "$it.gz" }
        }

    @TaskAction
    fun gzipFile() = ant.gzip(src = inputFile.get(), destfile = outputFile.get())

}

private fun AntBuilder.gzip(src: RegularFile, destfile: RegularFile) = withGroovyBuilder {
    "gzip"("src" to src.asFile, "destfile" to destfile.asFile)
}