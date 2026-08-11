import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
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
    private val pyprojectFile = uvProjectDir.file("pyproject.toml")

    @get:InputFile
    private val uvLockFile = uvProjectDir.file("uv.lock")

    init { globalArgs("sync") }

}