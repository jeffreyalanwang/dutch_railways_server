package com.jeffreyalanwang.dutchrailways.backend.routeQuery

internal inline fun <E> buildListReversed(builderAction: MutableList<E>.() -> Unit) =
    buildList(builderAction).asReversed()

internal fun <T> List<Collection<T>>.cartesianProduct(): Collection<List<T>> = buildList {
    // If zero digits
    if (this@cartesianProduct.isEmpty()) return@buildList

    // First digit
    addAll(this@cartesianProduct.first().map { listOf(it) })

    // Rest of the digits
    for (digitOptions in this@cartesianProduct.drop(1)) {
        for (i in this@buildList.indices.reversed()) {
            val partialList = this@buildList.removeAt(i)
            addAll(digitOptions.map { partialList + it })
        }
    }
}

/**
 * Set values in-place, if [predicate] returns `true`.
 *
 * Modifies [this] but not [from].
 *
 * @param from      Array of values with which to replace values in this array.
 *                  Should have the same length as [this]. If longer, excess values are ignored.
 * @param predicate Function to select the value to replace.
 */
internal fun <T> Array<T>.replaceIf(
    from: Array<T>,
    predicate: (currentValue: T, replaceBy: T) -> Boolean,
) {
    val other = from.iterator()

    for (i in indices) {
        val value = this[i]
        val replaceWith = other.next()
        if (predicate(value, replaceWith)) {
            this[i] = replaceWith
        }
    }
}

/**
 * Compares with the assumption that `null` equals positive infinity.
 */
internal infix fun <T: Comparable<T>> T?.finiteAndLt(other: T?) =
    this != null &&
    (other == null || this < other)

internal fun <T> List<T>.indexOfOrNull(element: T) =
    indexOf(element).takeUnless { it < 0 }

/**
 * Returns the index and value of the key in the map.
 *
 * If the key is not in the map, it is first initialized using [block].
 *
 * @receiver    A linked hash map, because it preserves insertion order.
 */
internal fun <K, V> LinkedHashMap<K, V>.withIndexOrPut(key: K, block: (K) -> V): IndexedValue<V> =
    get(key)?.let {
        IndexedValue( keys.indexOf(key), it )
    } ?:
    block(key).let {
        val index = size
        put(key, it)
        IndexedValue(index, it)
    }
