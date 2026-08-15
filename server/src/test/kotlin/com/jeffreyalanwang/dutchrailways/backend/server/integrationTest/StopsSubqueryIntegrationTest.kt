package com.jeffreyalanwang.dutchrailways.backend.server.integrationTest

import com.jeffreyalanwang.dutch_railways.backend.database.testing.SampleDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entityList
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

@AutoConfigureHttpGraphQlTester
@SpringBootTest
@SampleDatabaseTest
class StopsSubqueryIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Suppress("GraphQLUnresolvedReference")
    @Language("GraphQL")
    val query = $$"""
        query PassServiceById($id: ID!, $stopsAfter: DateTime!, $stopsCount: Int!) {
            passServiceById(id: $id) {
                stops(after: $stopsAfter, maxCount: $stopsCount) {
                    ...Selection
                }
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val passServiceId = 470
    val earliest = LocalDate.of(2026, 5, 2).atTime(8, 30).toInstant(ZoneOffset.UTC)!!
    val count = 5

    val argMap = mapOf(
        "id" to passServiceId,
        "stopsAfter" to earliest.toString(),
        "stopsCount" to count,
    )

    @Test
    fun `Pass service stops are after earliest time`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Stop {
                arriveTime
                departTime
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.stops").entityList<Map<String, OffsetDateTime?>>()
            .get().forEach {
                assertTrue (
                    earliest < (it["departTime"] ?: it["arriveTime"]!!).toInstant()
                )
            }
    }

    @Test
    fun `Pass service stops reference the correct pass service`() {
        @Language("GraphQL")
        val fragment = """
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

        response.path("passServiceById.stops[*].passService.id").entityList<Int>()
            .get().forEach { assertEquals(passServiceId, it) }
    }
}
