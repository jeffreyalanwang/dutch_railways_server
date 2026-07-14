package com.jeffeyalanwang.dutchrailways.backend.server.processing

import kotlin.time.Instant

fun <T, R> Triple<T, T, T>.map(block: (T) -> R): Triple<R, R, R> =
    Triple(block(first), block(second), block(third))

fun Triple<Boolean, Boolean, Boolean>.all() = first && second && third

fun <T> Triple<T, T, T>.all(block: (T) -> Boolean) = map(block).all()

fun Triple<Int, Int, Int>.min() = minOf(first, second, third)

fun <A, B, C> zip(a: Iterable<A>, b: Iterable<B>, c: Iterable<C>): List<Triple<A, B, C>> {
    val iterables = Triple(a, b, c)

    val out = iterables
        .map { (it as? Collection)?.size ?: 10 }.min()
        .let { estimatedCapacity -> ArrayList<Triple<A, B, C>>(estimatedCapacity) }

    iterables
        .let { (a, b, c) -> Triple(a.iterator(), b.iterator(), c.iterator()) } // do not use [map] to satisfy type system
        .run {
            while (all { it.hasNext() }) {
                val next = Triple(first.next(), second.next(), third.next()) // do not use [map] to satisfy type system
                out.add(next)
            }
        }

    return out
}

fun <T> List<Collection<T>>.cartesianProduct(): Collection<List<T>> = buildList {
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

class ClosedTimeRange(
    override val start: Instant,
    override val endInclusive: Instant
) : ClosedRange<Instant> {

    override fun contains(value: Instant): Boolean = value >= start && value <= endInclusive
    override fun isEmpty(): Boolean = !(start <= endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is ClosedTimeRange && (isEmpty() && other.isEmpty() ||
                start == other.start && endInclusive == other.endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()
    }

    override fun toString(): String = "$start..$endInclusive"
}

operator fun Instant.rangeTo(that: Instant) =
    ClosedTimeRange(this, that)

/**
 * Set values in-place, if [predicate] returns `true`.
 *
 * Modifies [this] but not [from].
 *
 * @param from      Array of values with which to replace values in this array.
 *                  Should have the same length as [this]. If longer, excess values are ignored.
 * @param predicate Function to select the value to replace.
 */
fun <T> Array<T>.replaceIf(
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
infix fun <T: Comparable<T>> T?.finiteAndLt(other: T?) =
    this != null &&
    (other == null || this < other)

/**
 * Returns the index of the key in the map.
 *
 * If the key is not in the map, it is first initialized using [block].
 *
 * @receiver    Must preserve insertion order.
 */
fun <K, V> MutableMap<K, V>.indexOfOrPut(key: K, block: (K) -> V): Int {
    var index = keys.indexOf(key)
    if (index < 0) {
        index = keys.size
        this[key] = block(key)

        // This assert is here because some implementations of
        // [MutableMap] might not provide ordered keys.
        check(keys.last() == key)
    }
    return index
}