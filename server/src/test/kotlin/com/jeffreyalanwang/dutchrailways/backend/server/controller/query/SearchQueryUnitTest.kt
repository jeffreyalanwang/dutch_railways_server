package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.backend.server.controller.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchCursorStrategy
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import tools.jackson.databind.ObjectMapper
import kotlin.test.Test

@GraphQlTest(SearchQueryController::class)
@Import(GraphQlConfig::class)
class SearchQueryUnitTest {
    @TestConfiguration
    class Config {
        @Bean fun encodingSearchCursorStrategy(mapper: ObjectMapper) = SearchCursorStrategy(mapper)
        @Bean fun searchService() = mockk<SearchService>()
    }
    @Autowired private lateinit var graphQlTester: GraphQlTester
    @Autowired private lateinit var searchService: SearchService
    @Autowired private lateinit var controller: SearchQueryController

    @Language("GraphQL")
    val querySearch = $$"""
        query($first: Int!, $request: SearchRequest!) {
            searchQuery(first: $first, request: $request) {
                pageInfo {
                    cursor
                    hasNextPage
                }
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @Language("GraphQL")
    val queryLoadMore = $$"""
        query($next: Int!, $cursor: ID!) {
            searchLoadMore(next: $next, cursor: $cursor) {
                pageInfo {
                    cursor
                    hasNextPage
                }
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @BeforeEach
    fun setUp() {
        every {
            searchService.search<Any>(any(), any(), any(), any(), any())
        } returns flowOf(
            Station(id = 1, name = "Station One"),
            Station(id = 2, name = "Station Two"),
        )
    }

    @Test
    fun `Single query`() {
        graphQlTester
            .document(querySearch)
            .variable("first", 100)
            .variable("request", mapOf(
                "anyLike" to "",
                "nameLike" to "",
                "near" to "",
            ))
            .execute()
            .path("searchQuery.pageInfo.cursor").valueIsNull()
            .path("searchQuery.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(false)
    }

    @Test
    fun `Continue paginated search`() {

        val response = graphQlTester
            .document(queryLoadMore)
            .variable("next", 1000)
            .variable("cursor", "")
            .execute()

        response
            .path("searchLoadMore.pageInfo.cursor").valueIsNull()
            .path("searchLoadMore.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(false)
    }
}