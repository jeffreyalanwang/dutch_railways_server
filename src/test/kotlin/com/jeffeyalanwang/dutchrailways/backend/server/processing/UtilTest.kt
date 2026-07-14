package com.jeffeyalanwang.dutchrailways.backend.server.processing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class UtilTest {

    @Test
    fun testTripleMap() {
        val triple = Triple(1, 2, 3)
        val mapped = triple.map { it * 2 }
        assertEquals(Triple(2, 4, 6), mapped)
    }

    @Test
    fun testTripleAllWithoutBlock() {
        assertTrue(Triple(true, true, true).all())
        assertFalse(Triple(true, false, true).all())
        assertFalse(Triple(false, false, false).all())
    }

    @Test
    fun testTripleAllWithBlock() {
        val triple1 = Triple("apple", "banana", "cherry")
        assertTrue(triple1.all { it.length > 3 })

        val triple2 = Triple("apple", "pie", "cherry")
        assertFalse(triple2.all { it.length > 3 })
    }

    @Test
    fun testTripleMin() {
        assertEquals(3, Triple(5, 3, 8).min())
        assertEquals(-1, Triple(-1, 0, 1).min())
    }

    @Test
    fun testZip() {
        val a = listOf(1, 2, 3)
        val b = listOf("x", "y", "z")
        val c = listOf(true, false, true)

        val zipped = zip(a, b, c)
        assertEquals(3, zipped.size)
        assertEquals(Triple(1, "x", true), zipped[0])
        assertEquals(Triple(2, "y", false), zipped[1])
        assertEquals(Triple(3, "z", true), zipped[2])
    }

    @Test
    fun testZipWithDifferentSizes() {
        val a = listOf(1, 2)
        val b = listOf("x", "y", "z")
        val c = listOf(true)

        val zipped = zip(a, b, c)
        assertEquals(1, zipped.size)
        assertEquals(Triple(1, "x", true), zipped[0])
    }

    @Test
    fun testCartesianProduct() {
        val input = listOf(
            listOf(1, 2),
            listOf(3, 4)
        )
        val product = input.cartesianProduct()
        assertEquals(4, product.size)
        assertTrue(product.contains(listOf(1, 3)))
        assertTrue(product.contains(listOf(1, 4)))
        assertTrue(product.contains(listOf(2, 3)))
        assertTrue(product.contains(listOf(2, 4)))
    }

    @Test
    fun testCartesianProductEmpty() {
        val input = emptyList<List<Int>>()
        val product = input.cartesianProduct()
        assertTrue(product.isEmpty())
    }

    @Test
    fun testClosedTimeRange() {
        val start = Instant.fromEpochMilliseconds(1000)
        val end = Instant.fromEpochMilliseconds(2000)
        val range = ClosedTimeRange(start, end)

        assertFalse(range.isEmpty())
        assertTrue(range.contains(start))
        assertTrue(range.contains(end))
        assertTrue(range.contains(Instant.fromEpochMilliseconds(1500)))
        assertFalse(range.contains(Instant.fromEpochMilliseconds(500)))
        assertFalse(range.contains(Instant.fromEpochMilliseconds(2500)))

        val rangeStr = range.toString()
        assertTrue(rangeStr.contains("1970-01-01T00:00:01Z"))

        val emptyRange = ClosedTimeRange(end, start)
        assertTrue(emptyRange.isEmpty())

        assertEquals(range, ClosedTimeRange(start, end))
        assertNotEquals(range, emptyRange)
        assertEquals(ClosedTimeRange(end, start), ClosedTimeRange(Instant.fromEpochMilliseconds(3000), Instant.fromEpochMilliseconds(1000))) // both empty are equal
    }

    @Test
    fun testInstantRangeTo() {
        val start = Instant.fromEpochMilliseconds(1000)
        val end = Instant.fromEpochMilliseconds(2000)
        val range = start..end
        assertTrue(range is ClosedTimeRange)
        assertEquals(start, range.start)
        assertEquals(end, range.endInclusive)
    }

    @Test
    fun testArrayReplaceIf() {
        val arr = arrayOf(1, 2, 3, 4)
        val from = arrayOf(5, 1, 6, 2)
        // Replace if [from] is larger
        arr.replaceIf(from) { current, replaceWith -> replaceWith > current }
        assertArrayEquals(arrayOf(5, 2, 6, 4), arr)
    }

    @Test
    fun testFiniteAndLt() {
        // null as infinity
        val a = 5
        val b = 10
        val n = null

        assertTrue(a finiteAndLt b)
        assertFalse(b finiteAndLt a)
        assertTrue(a finiteAndLt n)
        assertFalse(n finiteAndLt a)
        assertFalse(n finiteAndLt n)
    }

    @Test
    fun testMutableMapIndexOfOrPut() {
        val map = mutableMapOf<String, Int>()
        assertEquals(0, map.indexOfOrPut("a") { 10 })
        assertEquals(1, map.indexOfOrPut("b") { 20 })
        assertEquals(0, map.indexOfOrPut("a") { 30 }) // already exists, index 0, value unmodified

        assertEquals(10, map["a"])
        assertEquals(20, map["b"])
    }
}
