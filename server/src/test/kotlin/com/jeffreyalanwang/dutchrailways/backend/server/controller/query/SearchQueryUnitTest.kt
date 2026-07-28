package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.backend.server.controller.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchCursorStrategy
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchCursorStrategyConfig
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
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
@Import(GraphQlConfig::class, SearchCursorStrategyConfig::class)
class SearchQueryUnitTest {
    @TestConfiguration
    class Config {
        @Bean fun encodingSearchCursorStrategy(mapper: ObjectMapper) = SearchCursorStrategy(mapper)
    }
    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkBean private lateinit var searchService: SearchService
    @Autowired private lateinit var controller: SearchQueryController
    @MockkBean private lateinit var stationController: StationQueryController

    @Language("GraphQL")
    val querySearch = $$"""
        query($first: Int!, $request: SearchRequest!) {
            searchQuery(first: $first, request: $request) {
                pageInfo {
                    cursor
                    hasNextPage
                }
                results {
                    station {
                        id
                        name
                    }                
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
                results {
                    station {
                        id
                        name
                    }                
                }
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @BeforeEach
    fun setUp() {
        every {
            searchService.search<Any>(any(), any(), any(), any(), any())
        } returns (0..<100).asFlow().map { i ->
            Station(id = i, name = "Station $i")
        }
    }

    @Test
    fun `Single query`() {
        graphQlTester
            .document(querySearch)
            .variable("first", 5)
            .variable("request", mapOf(
                "anyLike" to "",
                "nameLike" to "",
                "near" to null,
            ))
            .execute()
            .path("searchQuery.pageInfo.cursor").hasValue()
            .path("searchQuery.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(true)
    }

    @Test
    fun `Single large query`() {
        graphQlTester
            .document(querySearch)
            .variable("first", 100)
            .variable("request", mapOf(
                "anyLike" to "",
                "nameLike" to "",
                "near" to null,
            ))
            .execute()
            .path("searchQuery.pageInfo.cursor").valueIsNull()
            .path("searchQuery.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(false)
    }

    @Test
    fun `Continue paginated search`() {
        val cursor = graphQlTester
            .document(querySearch)
            .variable("first", 5)
            .variable("request", mapOf(
                "anyLike" to "",
                "nameLike" to "",
                "near" to null,
            ))
            .execute()
            .path("searchQuery.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(true)
            .path("searchQuery.pageInfo.cursor").entity<String>().get()

        graphQlTester
            .document(queryLoadMore)
            .variable("next", 100)
            .variable("cursor", cursor)
            .execute()
            .path("searchLoadMore.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(false)
            .path("searchLoadMore.pageInfo.cursor").valueIsNull()
    }
}