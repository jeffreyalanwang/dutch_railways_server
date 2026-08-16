package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import com.jeffreyalanwang.dutchrailways.backend.database.testing.SampleDatabaseTest

@SpringBootTest
@SampleDatabaseTest
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
    fun `Query for PassService with anyLike`() = runBlocking {
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
    fun `Query for Station with nameLike`() = runBlocking {
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
    fun `Query for Station with typo in nameLike`() = runBlocking {
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
    @ParameterizedTest
    @CsvSource(
        "Eindhoven Centraal , 51.44333267202848 , 5.481389125142196",
        "Rotterdam Centraal , 51.92499923833712 , 4.468888827643444",
        "Apeldoorn          , 52.20916748798910 , 5.970277864627025",
    )
    fun `Query with nearby GeoRect`(name: String, latitude: Double, longitude: Double) = runBlocking {
        val results = searchService.search(
            anyLike = null,
            nameLike = name.takeLast(4),
            near = GeoRect(
                northwest = GeoCoords(latitude = latitude + 0.005, longitude = longitude - 0.005),
                southeast = GeoCoords(latitude = latitude - 0.005, longitude = longitude + 0.005),
            ),
            types = listOf(Station::class),
            batchSize = 10,
        ).onEach { assertIs<Station>(it) }
            .toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.size > 1) // GeoRect should bias, but not limit
        assertEquals(name, results.first().name)
    }
}