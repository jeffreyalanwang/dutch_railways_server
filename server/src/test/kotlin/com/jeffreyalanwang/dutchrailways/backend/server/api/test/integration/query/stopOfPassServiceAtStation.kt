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
class `Integration test for stopOfPassServiceAtStation query`(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Language("GraphQL")
    val query = $$"""
        query StopOfPassServiceAtStation($passService: ID!, $station: ID!) {
            stopOfPassServiceAtStation(passService: $passService, station: $station) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val passServiceId = 470
    val stationId = 1229

    val argMap = mapOf(
        "passService" to passServiceId,
        "station" to stationId,
    )

    @Test
    fun `Returns a stop`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Stop {
                arriveTime
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stopOfPassServiceAtStation").hasValue()
    }

    @Test
    fun `Stop has arrive time`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Stop {
                arriveTime
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stopOfPassServiceAtStation.arriveTime").hasValue()
    }

    @Test
    fun `Stop references the correct pass service`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Stop {
                passService {
                    id
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stopOfPassServiceAtStation.passService.id").entity<Int>().isEqualTo(passServiceId)
    }

    @Test
    fun `Stop references the correct station`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Stop {
                station {
                    id
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("stopOfPassServiceAtStation.station.id").entity<Int>().isEqualTo(stationId)
    }
}
