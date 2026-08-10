import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import javax.inject.Inject
import com.pswidersk.gradle.python.uv.UvTask

/**
 * A [UvTask] with protected top-level args (those passed first, directly to `uv`).
 * Forces user-provided arguments to be appended after a protected argument provider.
 */
@CacheableTask
abstract class UvGlobalArgsTask @Inject constructor(
    objects: ObjectFactory
) : UvTask(objects) {

    init {
        argumentProviders.add { getGlobalArgs() }
        argumentProviders.add { getArgs() }
    }

    private val globalArgs = mutableListOf<Any>()
    protected fun globalArgs(args: Iterable<Any>) = apply { globalArgs.addAll(args) }
    protected fun globalArgs(vararg args: Any) = apply { globalArgs.addAll(args) }
    private fun getGlobalArgs() = readArgs(globalArgs)

    private var userArgs = mutableListOf<Any>()
    override fun args(args: Iterable<*>) = apply { userArgs.addAll(args.filterNotNull()) }
    override fun args(vararg args: Any) = apply { userArgs.addAll(args) }
    override fun setArgs(arguments: Iterable<*>) = apply { userArgs = arguments.filterNotNull().toMutableList() }
    override fun setArgs(arguments: List<String>) = setArgs(arguments as Iterable<*>)
    override fun getArgs() = readArgs(userArgs)
}

private fun Any.unwrapIfProvider() = (this as? Provider<*>)?.get() ?: this
private fun readArg(arg: Any) = arg.unwrapIfProvider().toString()
private fun readArgs(args: Iterable<Any>) = args.map { readArg(it) }