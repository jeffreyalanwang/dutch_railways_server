package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.util.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs

@SpringBootTest
class SearchServiceTest(
    @Autowired private val searchService: SearchService,
) {
    @BeforeEach
    fun setUp() = runBlocking {
        // Wait for initial indexing to complete
        searchService.initJob.join()
    }

    @Transactional
    @Test
    fun `Query for PassService with anyLike`(): Unit = runBlocking {
        val results = searchService.search(
            anyLike = "Intercity",
            nameLike = null,
            near = null,
            types = listOf(PassService::class),
            batchSize = 10,
        ).toList().run {
            map { assertIs<PassService>(it); it }
        }

        assertTrue(results.size > 1)
        assertContains(results.first().name, "Intercity")
    }

    @Transactional
    @Test
    fun `Query for Station with nameLike`(): Unit = runBlocking {
        val results = searchService.search(
            anyLike = null,
            nameLike = "Eindhoven Centaral",
            near = null,
            types = listOf(Station::class),
            batchSize = 10,
        ).toList().run {
            map { assertIs<Station>(it); it }
        }

        assertTrue(results.isNotEmpty())
        assertEquals("Eindhoven Centraal", results.first().name)
    }

    @Transactional
    @Test
    fun `Query for Station with typo in nameLike`(): Unit = runBlocking {
        val results = searchService.search(
            anyLike = null,
            nameLike = "Centarl",
            near = null,
            types = listOf(Station::class),
            batchSize = 10,
        ).toList().run {
            map { assertIs<Station>(it); it }
        }

        assertTrue(results.size > 1)
        results.forEach {
            assertContains(it.name, "Centraal")
        }
    }

    @Transactional
    @Test
    fun `Query for Station with nearby GeoRect`(): Unit = runBlocking {
        val results = searchService.search(
            anyLike = null,
            nameLike = "Centraal",
            near = GeoRect(
                northwest = GeoCoords(latitude = 51.442, longitude = 5.480),
                southeast = GeoCoords(latitude = 51.444, longitude = 5.482),
            ),
            types = listOf(Station::class),
            batchSize = 10,
        ).toList().run {
            map { assertIs<Station>(it); it }
        }

        assertTrue(results.size > 1)
        assertEquals("Eindhoven Centraal", results.first().name)
    }
}