package com.jeffreyalanwang.dutchrailways.backend.server.repository

import kotlin.test.Test
import kotlin.test.assertContentEquals

class UtilTest {

    @Test
    fun `findInOrderIn() selects the correct order`() {
        val sequence = sequenceOf (
            Triple(1, 1, 1),
            Triple(2, 1, 2),
            Triple(2, 2, 3),
            Triple(3, 2, 4),
            Triple(3, 3, 5),
            Triple(4, 3, 6),
        )
        val order = listOf(
            Pair(3, 3),
            Pair(4, 3),
            Pair(2, 1),
        )

        val result = sequence.findInOrderIn(order) { Pair(it.first, it.second) }

        assertContentEquals(
            listOf(5, 6, 2),
            result.map { it.third },
        )
    }

}