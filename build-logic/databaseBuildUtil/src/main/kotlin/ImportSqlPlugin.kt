import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.testcontainers.gradle.TestcontainersPlugin

class ImportSqlPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply<TestcontainersPlugin>()
    }
}