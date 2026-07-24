package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class QueryStationByIdIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Language("GraphQL")
    val query = $$"""
        query StationById($id: ID!) {
            stationById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val stationId = 1229

    val argMap = mapOf(
        "id" to stationId
    )

    @Test
    fun `Returns a station`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Station {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stationById")
            .hasValue()
    }

    @Test
    fun `Station has expected id`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Station {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stationById.id").entity<Int>()
            .isEqualTo(stationId)
    }

    @Test
    fun `Station returns basic details`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Station {
                name
                address
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stationById.name").hasValue()
        response.path("stationById.address").hasValue()
    }

    @Test
    fun `Station returns geometry information`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Station {
                geom {
                    latitude
                    longitude
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()
            .errors()
            .verify()

        response.path("stationById").hasValue()
        response.path("stationById.geom").hasValue()
        response.path("stationById.geom.latitude").hasValue()
        response.path("stationById.geom.longitude").hasValue()
    }

    @Test
    fun `Station is nested within expected parent areas`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Area {
            locatedIn {
                    name
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("areaById.locatedIn.name").entity<String>().matches { it.isNotEmpty() }
    }
}
