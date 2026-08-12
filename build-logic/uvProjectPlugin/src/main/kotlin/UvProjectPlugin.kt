import com.pswidersk.gradle.python.uv.UvPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class UvProjectPlugin : Plugin<Project> {

    abstract class Extension {
        abstract val uvProjectDir: DirectoryProperty
        internal operator fun component1() = uvProjectDir
    }

    override fun apply(target: Project): Unit = target.run {
        apply<UvPlugin>()
        apply<JavaBasePlugin>()

        // Configure uv project directory (containing `pyproject.toml` to sync to)
        val (uvProjectDirValue) = extensions.create<Extension>("uvProject").apply {
            uvProjectDir.convention(layout.projectDirectory)
        }
        afterEvaluate {
            // Register as a source set so IntelliJ sees it as a module
            extensions.getByType<SourceSetContainer>().create(uvProjectDirValue.name.get())
        }

        // Register a single sync task for the declared project
        val syncTask = tasks.register<UvSyncTask>("syncUvProject")

        // Associate all [UvProjectTask]s with the project directory,
        // and configure (if requested) dependency on the sync task
        tasks.withType<UvProjectTask>().configureEach {
            uvProjectDir = uvProjectDirValue
        }
        afterEvaluate {
            tasks.withType<UvProjectTask>().configureEach {
                if (requiresSync.get()) dependsOn(syncTask)
            }
        }
    }

}

private val DirectoryProperty.name get() = map { it.asFile.name }