package com.jeffreyalanwang.dutchrailways.backend.server.repository

import org.springframework.dao.DuplicateKeyException

/**
 * For a flat sequence which already contains items grouped by key,
 * de-flattens into a sequence of lists.
 */
fun <T, K> Sequence<T>.deflattenBy(keySelector: (T) -> K) = sequence {
    var curr: Pair<K, MutableList<T>>? = null
    forEach { v ->
        val k = keySelector(v)
        if (curr == null || curr.first != k) {
            curr?.let { yield(it) }
            curr = Pair(k, mutableListOf())
        }
        curr.second.add(v)
    }
    curr?.let { yield(it) }
}

fun <K, V: Any> Collection<K>.slotsMap() = LinkedHashMap<K, V?>(size).also { associateWithTo(it) { null } }

/**
 * Returns values in this sequence, in the same order
 * as their keys in the provided [keys] list.
 *
 * Items in [this] sequence are filtered out if they do not have a key in [keys].
 * Items in [keys] sequence are mapped to `null` if they do not have a value in [this].
 *
 * @throws DuplicateKeyException if multiple items map to the same key in [keys].
 */
fun <K, V: Any> Sequence<V>.joinedOn(keys: List<K>, selector: (V) -> K) =
    keys.slotsMap<K, V>()
    .also { map ->
        for ((key, item) in this.map { item -> selector(item) to item }) {
            when {
                key !in map      -> continue
                map[key] != null -> throw DuplicateKeyException(null)
                else             -> map[key] = item
            }
        }
    }
    .sequencedValues().toList()

fun <K, V: Any> Iterable<V>.joinedOn(keys: List<K>, selector: (V) -> K) =
    associateBy { selector(it) }.run {
        keys.map { key -> getOrDefault(key, null) }
    }

