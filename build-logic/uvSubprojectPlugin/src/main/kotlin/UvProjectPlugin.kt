import com.pswidersk.gradle.python.uv.UvExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

class UvProjectPlugin : Plugin<Project> {

    abstract class Extension {
        abstract val uvProjectDir: DirectoryProperty
    }

    override fun apply(target: Project): Unit = target.run {
        plugins.apply("com.pswidersk.python-uv-plugin")

        val extension = extensions.create<Extension>("uvProject").apply {
            uvProjectDir.convention(layout.projectDirectory)
        }

        val syncTask = tasks.register<UvSyncTask>("syncUvProject")

        tasks.withType<UvProjectTask>().configureEach {
            uvProjectDir = extension.uvProjectDir
        }
        afterEvaluate {
            tasks.withType<UvProjectTask>().configureEach {
                if (requiresSync.get()) dependsOn(syncTask)
            }
        }
    }

}
