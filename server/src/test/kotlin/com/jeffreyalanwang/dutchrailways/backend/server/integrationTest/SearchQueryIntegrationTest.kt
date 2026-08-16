package com.jeffreyalanwang.dutchrailways.backend.server.integrationTest

import com.jeffreyalanwang.dutchrailways.backend.database.testing.SampleDatabaseTest
import com.jeffreyalanwang.dutchrailways.api.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.GeoRect
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import org.testcontainers.containers.ExecConfig
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test

@AutoConfigureHttpGraphQlTester
@SpringBootTest
@SampleDatabaseTest
class SearchQueryIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
    @Autowired private val dbContainer: PostgreSQLContainer,
) {

    val anyLike = "Uraniumweg" // Street name of Heerenveen IJsstadion
    val nameLike = "Centarl"
    val near = GeoCoords( // Location of Eindhoven Centraal
        latitude = 51.44278,
        longitude = 5.47972,
    ).let {
        GeoRect(it, it)
    }

    @Suppress("GraphQLUnresolvedReference")
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

    init {
        dbContainer.execInContainer(ExecConfig.builder().user(dbContainer.username)
            .command(arrayOf("psql", "-c", "ALTER DATABASE postgres SET jit = off;"))
            .build())
    }

    @Test
    fun `Search all using anyLike`() {
        val response = graphQlTester
            .document(query)
            .variable("first", 100)
            .variable("request", mapOf(
                "anyLike" to anyLike,
            ))
            .fragment(fragments.all)
            .execute()

        response.path("searchQuery.results[*].*.name")
            .entityList<String>()
            .hasSize(1)
    }

    @Test
    fun `Search using nameLike`() {
        val response = graphQlTester
            .document(query)
            .variable("first", 1000)
            .variable("request", mapOf(
                "nameLike" to nameLike,
            ))
            .fragment(fragments.station)
            .execute()

        response.path("searchQuery.results[*].station.id").entityList<Int>().hasSizeGreaterThan(0)
    }

    @Test
    fun `Search all using nameLike and near`() {
        val response = graphQlTester
            .document(query)
            .variable("first", 1000)
            .variable("request", mapOf(
                "nameLike" to nameLike,
                "near" to near,
            ))
            .fragment(fragments.station)
            .execute()

        response.path("searchQuery.results[0].station.name")
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