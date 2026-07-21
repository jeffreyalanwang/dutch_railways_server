package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class `Integration test for passServiceById query`(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Language("GraphQL")
    val query = $$"""
        query PassServiceById($id: ID!) {
            passServiceById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val passServiceId = 470

    val argMap = mapOf(
        "id" to passServiceId
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
    fun `Pass service stops reference the correct pass service`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on PassService {
                stops {
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
            .hasSizeGreaterThan(1)
            .get().forEach { assertEquals(passServiceId, it) }
    }
}
