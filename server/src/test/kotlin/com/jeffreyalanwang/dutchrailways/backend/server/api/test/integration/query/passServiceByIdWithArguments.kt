package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import kotlin.test.assertEquals
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class `Integration test for passServiceById query with arguments`(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Language("GraphQL")
    val query = $$"""
        query PassServiceById($id: ID!, $stopsAfter: DateTime, $stopsCount: Int) {
            passServiceById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val passServiceId = 470
    val earliest = LocalDate.orNull(2026, 5, 1)!!.atStartOfDayIn(TimeZone.UTC)
    val count = 5

    val argMap = mapOf(
        "id" to passServiceId,
        "stopsAfter" to earliest.toString(),
        "stopsCount" to count,
    )

    @Test
    fun `Returns a pass service`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById")
            .hasValue()
    }

    @Test
    fun `Pass service has expected id`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.id").entity<Int>()
            .isEqualTo(passServiceId)
    }

    @Test
    fun `Pass service has name containing digits`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                name
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.name").entity<String>()
            .matches { it.count { it.isDigit() } >= 3 }
    }

    @Test
    fun `Pass service has requested number of stops`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                stops(after: $stopsAfter, maxCount: $stopsCount) {
                    passService {
                        id
                    }
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.stops").entityList<Any>()
            .hasSize(count)
    }

    @Test
    fun `Pass service stops are after earliest time`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                stops(after: $stopsAfter, maxCount: $stopsCount) {
                    time
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.stops.time").entityList<Instant>()
            .get().forEach { assertTrue(earliest < it) }
    }

    @Test
    fun `Pass service stops reference the correct pass service`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                stops(after: $stopsAfter, maxCount: $stopsCount) {
                    passService {
                        id
                    }
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.stops.passService.id").entityList<Int>()
            .get().forEach { assertEquals(passServiceId, it) }
    }
}
