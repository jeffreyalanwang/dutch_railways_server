package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.replaceIf

/**
 * Holds a value for every [K] in the graph.
 */
@JvmInline
internal value class GraphAttribute<K: GraphId, V>(
    private val data: Array<V>
) {
    val size get() = data.size
    operator fun get(id: K) = data[id.index]
    operator fun set(id: K, value: V) = data.set(id.index, value)

    fun replaceIf(from: GraphAttribute<K, V>, predicate: (V, V) -> Boolean) =
        data.replaceIf(from.data, predicate)

    inline fun <reified R> map(transform: (V) -> R) =
        GraphAttribute<K, R>(
            Array(size) { i -> transform(data[i]) }
        )

    fun copy() = GraphAttribute<K, V>(data.copyOf())

    companion object {
        context(graph: TransitGraph)
        internal inline fun <reified K: GraphId, reified V> new(
            init: (StationId) -> V
        ): GraphAttribute<K, V> {
            val data = Array(graph.countOf<K>()) { init(StationId(it)) }
            return GraphAttribute(data)
        }

        context(graph: TransitGraph)
        internal inline fun <reified K: GraphId, reified V> new(
            init: Pair<K, V>?,
            default: () -> V,
        ) = new<K, V> { default() }
            .also {
                init?.run { it[first] = second }
            }
    }
}
