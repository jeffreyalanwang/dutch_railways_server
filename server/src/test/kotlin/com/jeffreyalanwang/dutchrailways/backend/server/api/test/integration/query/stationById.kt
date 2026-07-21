package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class `Integration test for stationById query`(
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
        val fragment = $$"""
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
        val fragment = $$"""
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
        val fragment = $$"""
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
        val fragment = $$"""
            fragment Selection on Station {
                geom {
                    latitude
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stationById.geom.latitude").hasValue()
    }
}
