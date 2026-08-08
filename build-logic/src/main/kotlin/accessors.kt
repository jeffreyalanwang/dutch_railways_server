import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// Naming the root project's version catalog `libs` would make more sense,
// but seems to cause issues for hosting modules which try to access their
// normal reference to the catalog using same member name.
val Project.rootLibs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.plugin(alias: String) = findPlugin(alias).get()
fun VersionCatalog.library(alias: String) = findLibrary(alias).get()
fun VersionCatalog.bundle(alias: String) = findBundle(alias).get()

fun PluginContainer.apply(aliasProvider: Provider<PluginDependency>) = apply(aliasProvider.get().pluginId)

fun Project.kotlin(configuration: KotlinJvmProjectExtension.() -> Unit) = configure<KotlinJvmProjectExtension>(configuration)

fun DependencyHandler.testImplementation(dependencyNotation: Provider<*>) = add("testImplementation", dependencyNotation)
