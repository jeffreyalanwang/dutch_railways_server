package com.jeffreyalanwang.dutchrailways.backend.server.controller.test.integration.query

import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.util.GeoRect
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import kotlin.test.Test

@AutoConfigureHttpGraphQlTester
@SpringBootTest
class SearchQueryIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {

    val anyLike = "Uraniumweg" // Street name of Heerenveen IJsstadion
    val nameLike = "Centarl"
    val near = GeoCoords( // Location of Eindhoven Centraal
        latitude = 51.44278,
        longitude = 5.47972,
    ).let {
        GeoRect(it, it)
    }

    @Language("GraphQL")
    val query = $$"""
        query($first: Int!, $request: SearchRequest!) {
            searchQuery(first: $first, request: $request) {
                ...Selection
            }
        }
    """.trimIndent()

    object fragments {
        @Language("GraphQL")
        val all = """
        fragment Selection on SearchResults {
            pageInfo {
                cursor
                hasNextPage
            }
            results {
                passService {
                    id
                }
                area {
                    id
                }
                station {
                    id
                    name
                }
            }
        }
        """.trimIndent()

        @Language("GraphQL")
        val station = """
            fragment Selection on SearchResults {
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
        """.trimIndent()
    }

    @Test
    fun `Search all using anyLike`() {
        graphQlTester
            .document(query)
            .variable("first", 100)
            .variable("request", mapOf(
                "anyLike" to anyLike,
            ))
            .fragment(fragments.all)
            .execute()
            .path("searchQuery.results.*.name")
            .entityList<String>()
            .hasSize(1)
    }

    @Test
    fun `Search using nameLike`() {
        graphQlTester
            .document(query)
            .variable("first", 1000)
            .variable("request", mapOf(
                "nameLike" to nameLike,
            ))
            .fragment(fragments.station)
            .execute()
            .path("searchQuery.results[*].station.id").entityList<Int>().hasSizeGreaterThan(0)
    }

    @Test
    fun `Search all using nameLike and near`() {
        graphQlTester
            .document(query)
            .variable("first", 1000)
            .variable("request", mapOf(
                "nameLike" to nameLike,
                "near" to near,
            ))
            .execute()
            .path("searchQuery.results[0].station.name")
            .entity<String>().equals("Eindhoven Centraal")
    }

    @Test
    fun `Continue paginated search`() {

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
                        }                    
                    }
                }    
            }
        """.trimIndent()

        val response1 = graphQlTester
            .document(query)
            .variable("first", 2)
            .variable("request", mapOf(
                "nameLike" to nameLike
            ))
            .fragment(fragments.station)
            .execute()

        val cursor = response1
            .path("searchQuery.results[*].station.id").entityList<Int>().hasSize(2)
            .path("searchQuery.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(true)
            .path("searchQuery.pageInfo.cursor").entity<String>().get()

        val response2 = graphQlTester
            .document(queryLoadMore)
            .variable("next", 1000)
            .variable("cursor", cursor)
            .execute()

        response2
            .path("searchLoadMore.pageInfo.hasNextPage").entity<Boolean>().isEqualTo(false)
            .path("searchLoadMore.results[*].station.id").entityList<Int>().hasSizeGreaterThan(2)
    }

}