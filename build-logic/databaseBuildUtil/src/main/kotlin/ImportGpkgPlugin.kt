import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.testcontainers.containers.GenericContainer
import org.testcontainers.gradle.TestcontainersExtension
import org.testcontainers.gradle.TestcontainersPlugin
import org.testcontainers.gradle.getContainer

internal const val GDAL_CONTAINER_NAME = "gdal"

class ImportGpkgPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = target.run {
        apply<TestcontainersPlugin>()

        extensions.configure<TestcontainersExtension> {
            genericContainer(GDAL_CONTAINER_NAME) {
                image("ghcr.io/osgeo/gdal:ubuntu-small-latest")
            }
        }
        val gdalContainerImpl = extensions.getByType<TestcontainersExtension>()
            .getContainer<GenericContainer<*>>(GDAL_CONTAINER_NAME)

        tasks.withType<DbImportGpkgTask>().configureEach {
            gdalContainer = gdalContainerImpl
        }
    }

}
