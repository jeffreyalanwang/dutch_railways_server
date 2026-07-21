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
class `Integration test for areaById query`(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Language("GraphQL")
    val query = $$"""
        query AreaById($id: ID!) {
            areaById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val areaId = 168

    val argMap = mapOf(
        "id" to areaId
    )

    @Test
    fun `Returns an area`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Area {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("areaById").hasValue()
    }

    @Test
    fun `Area has expected id`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Area {
                id
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("areaById.id").entity<Int>().isEqualTo(areaId)
    }

    @Test
    fun `Area is nested within expected parent areas`() {
        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Area {
                locatedIn {
                    locatedIn {
                        name
                    }
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("areaById.locatedIn.locatedIn.name").entity<String>().isEqualTo("unknown")
    }
}
