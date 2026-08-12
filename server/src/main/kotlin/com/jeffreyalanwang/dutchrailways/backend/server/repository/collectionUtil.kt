package com.jeffreyalanwang.dutchrailways.backend.server.repository

import org.springframework.dao.DuplicateKeyException
import java.util.*

/**
 * For a flat sequence which already contains items grouped by key,
 * de-flattens into a sequence of lists.
 */
internal fun <T, K> Sequence<T>.deflattenBy(keySelector: (T) -> K) = sequence {
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

internal fun <K, V: Any> Collection<K>.slotsMap() = LinkedHashMap<K, V?>(size).also { associateWithTo(it) { null } }

/**
 * Returns values in this sequence, in the same order
 * as their keys in the provided [keys] list.
 *
 * Items in [this] sequence are filtered out if they do not have a key in [keys].
 * Items in [keys] sequence are mapped to `null` if they do not have a value in [this].
 *
 * @throws DuplicateKeyException if multiple items map to the same key in [keys].
 */
internal fun <K, V: Any> Sequence<V>.joinedOn(keys: List<K>, selector: (V) -> K) =
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

internal fun <K, V: Any> Iterable<V>.joinedOn(keys: List<K>, selector: (V) -> K) =
    associateBy { selector(it) }.run {
        keys.map { key -> getOrDefault(key, null) }
    }

internal inline fun <reified K: Enum<K>, V> EnumMap() = EnumMap<K, V>(K::class.java)

internal fun <K, V> MutableMap<K, V>.getOrPutBulk(keys: Iterable<K>, defaultValueLoader: (List<K>) -> List<V>): Map<K, V> {
    val missed = mutableListOf<K>()
    val out = mutableMapOf<K, V>()

    for (key in keys) {
        if (key in this) {
            out[key] = this[key]!!
        } else {
            missed += key
        }
    }

    val rest = defaultValueLoader(missed)
    putAll(missed zip rest)
    out.putAll(missed zip rest)
    return out
}

internal class SetCompareBuilderScope<C> {
    private lateinit var keys: MutableSet<C>
    private var keysInitialized = false
    private var inequalityFound = false

    inline infix fun <T> Iterable<T>.on(selector: (T) -> C) = when {
        inequalityFound -> Unit

        !keysInitialized -> {
            keys = (this as? Collection)?.run { HashSet(size) } ?: HashSet()
            mapTo(keys, selector)
            keysInitialized = true
        }

        this is Collection && size != keys.size -> {
            inequalityFound = true
        }

        else -> {
            inequalityFound = any { selector(it) !in keys }
        }
    }

    context(receiver: Iterable<T>)
    inline infix fun <T> thisOn(selector: (T) -> C) = receiver on selector

    val itself = { it: C -> it }

    companion object {
        inline fun <C> allSetEqual(builder: SetCompareBuilderScope<C>.() -> Unit) =
            SetCompareBuilderScope<C>().apply(builder).inequalityFound.not()
    }
}
