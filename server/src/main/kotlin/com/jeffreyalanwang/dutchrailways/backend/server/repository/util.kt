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

/**
 * Returns values in this sequence, in the same order
 * as their keys in the provided [source] list.
 *
 * Items are not included if they are not in the
 * intersection of both [this] and [source].
 */
fun <S, T> Sequence<T>.byOrderIn(source: List<S>, selector: (T) -> S): List<T> =
    associateBy(selector).run {
        source.mapNotNull { this[it] }
    }

/**
 * Returns values in this sequence, in the same order
 * as their keys in the provided [source] list.
 *
 * Items in this sequence are filtered out if they do not have a key in [source].
 * @throws DuplicateKeyException if multiple items map to the same key in [source].
 * @throws NoSuchElementException if no items map to some key in [source].
 */
fun <S, T> Sequence<T>.findInOrderIn(source: List<S>, selector: (T) -> S): List<T> {
    val map = source.associateWithTo( LinkedHashMap(source.size) ) { null as T? }

    for (item in this) {
        val key = selector(item)
        when {
            key !in map      -> continue
            map[key] != null -> throw DuplicateKeyException(null)
            else             -> map[key] = item
        }
    }

    return map.map { (k, v) -> v ?: throw NoSuchElementException("No item present in sequence with key $k.") }
}