import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

@CacheableTask
abstract class UvSyncTask @Inject constructor(
    objects: ObjectFactory
) : UvProjectTask(objects) {

    init { description = "Initialize dependencies for Python scripts" }

    override val requiresSync = objects.property<Boolean>().apply {
        set(false)
        finalizeValue()
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    internal val pyprojectFile = uvProjectDir.file("pyproject.toml")

    @get:OutputFile
    internal val uvLockFile = uvProjectDir.file("uv.lock")

    init { globalArgs("sync") }

}