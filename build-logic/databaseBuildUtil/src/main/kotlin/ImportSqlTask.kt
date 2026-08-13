import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class ImportSqlTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DbImportTask(objectFactory) {

    @get:InputFiles
    abstract val initScripts: ListProperty<RegularFile>
    private val mountableInitScripts = initScripts.mapEach {
        it.asFile.asTestContainerMountable() to "/init_scripts/${it.asFile.name}"
    }

    @get:Internal abstract val resources: ListProperty<Pair<FileSystemLocation, String>>
    @get:Optional @get:InputFiles private val resourceSrcs = resources.map { it.unzip().first }
    @get:Optional @get:Input      private val resourceDests = resources.map { it.unzip().second }
    private val mountableResources = resources.mapEach { (file, containerPath) ->
        file.asFile.asTestContainerMountable() to containerPath
    }
    fun resource(file: Provider<out FileSystemLocation>, containerPath: String) = resources.add(
        file.map { it to containerPath }
    )

    @TaskAction
    fun importSql() = dbContainer.get().run {
        mountableResources.get().forEach { (mountable, destPath) ->
            copyFileToContainer(mountable, destPath)
        }
        mountableInitScripts.get().forEach { (mountable, destPath) ->
            copyFileToContainer(mountable, destPath)
            execInContainer(
                "sh", "-c",
                "cat $destPath | psql -d $dbName",
            )
        }
    }

}