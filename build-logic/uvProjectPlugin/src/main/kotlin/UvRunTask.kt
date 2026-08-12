import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class UvRunTask @Inject constructor(
    objects: ObjectFactory
) : UvProjectTask(objects) {

    init { globalArgs("run") }

}