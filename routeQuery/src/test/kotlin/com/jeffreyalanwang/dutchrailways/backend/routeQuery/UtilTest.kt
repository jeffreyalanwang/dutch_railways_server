package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UtilTest {

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
    fun testArrayReplaceIf() {
        val arr = arrayOf(1, 2, 3, 4)
        val from = arrayOf(5, 1, 6, 2)
        // Replace if [from] is larger
        arr.replaceIf(from) { current, replaceWith ->
            replaceWith > current
        }
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

}
