import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import javax.inject.Inject

@CacheableTask
abstract class UvRunTask @Inject constructor(
    objects: ObjectFactory
) : UvProjectTask(objects) {

    init { globalArgs("run") }

}