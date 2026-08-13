import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

@CacheableTask
abstract class UvProjectTask @Inject constructor(
    objects: ObjectFactory
) : UvGlobalArgsTask(objects) {

    @get:Internal
    open val requiresSync = objects.property<Boolean>()
        .convention(true)

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val uvProjectDir: DirectoryProperty

    private val uvProjectDirPath = uvProjectDir.map { it.asFile.path }

    init {
        globalArgs("--project", uvProjectDirPath)
    }
}
