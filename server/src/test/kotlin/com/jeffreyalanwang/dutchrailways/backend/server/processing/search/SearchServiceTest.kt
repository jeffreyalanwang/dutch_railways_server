package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs

@SpringBootTest
class SearchServiceTest(
    @Autowired private val searchService: SearchService,
) {
    @Transactional
    @Test
    fun `Query for PassService with anyLike`(): Unit = runBlocking {
        // Wait for initial indexing to complete
        searchService.initJob.join()

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
}