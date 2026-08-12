import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.configureEach
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.assign
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

        tasks.withType<ImportGpkgTask>().configureEach {
            gdalContainer = gdalContainerImpl
        }
    }

//    @get:Optional @get:OutputFile abstract val sqlDumpOutputFile: RegularFileProperty
//    @get:Optional @get:OutputFile abstract val pgDumpOutputFile: RegularFileProperty
//    private val isSqlDumpRequested get() = sqlDumpOutputFile.isPresent
//    private val isPgDumpRequested get() = pgDumpOutputFile.isPresent

}
