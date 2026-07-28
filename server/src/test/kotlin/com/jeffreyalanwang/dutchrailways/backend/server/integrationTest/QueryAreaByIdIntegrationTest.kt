package com.jeffreyalanwang.dutchrailways.backend.server.integrationTest

import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import kotlin.test.Test

@AutoConfigureHttpGraphQlTester
@SpringBootTest
class QueryAreaByIdIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Suppress("GraphQLUnresolvedReference")
    @Language("GraphQL")
    val query = $$"""
        query AreaById($id: ID!) {
            areaById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val areaId = 197

    val argMap = mapOf(
        "id" to areaId
    )

    @Test
    fun `Returns an area`() {
        @Language("GraphQL")
        val fragment = """
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
        val fragment = """
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
    fun `Area returns geometry information`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Area {
                geom {
                    polygons {
                        rings {
                            points {
                                latitude
                                longitude
                            }                    
                        }
                    }
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

        response.path("areaById").hasValue()
        response.path("areaById.geom").hasValue()
        response.path("areaById.geom.polygons[*]").hasValue().entityList<Any>().hasSizeGreaterThan(0)
        response.path("areaById.geom.polygons[*].rings").hasValue().entityList<Any>().hasSizeGreaterThan(0)
        response.path("areaById.geom.polygons[*].rings[*].points").hasValue().entityList<Any>().hasSizeGreaterThan(0)
        response.path("areaById.geom.polygons[*].rings[*].points[*].latitude").hasValue().entityList<Any>().hasSizeGreaterThan(0)
        response.path("areaById.geom.polygons[*].rings[*].points[*].longitude").hasValue().entityList<Any>().hasSizeGreaterThan(0)
    }

    @Test
    fun `Area is nested within expected parent areas`() {
        @Language("GraphQL")
        val fragment = """
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

        response.path("areaById.locatedIn[*].name")
            .entityList<String>()
            .matches<GraphQlTester.EntityList<String>> {
                it.all { name -> name.isNotEmpty() }
            }
            .hasSizeGreaterThan(1)
    }

    @Test
    fun `Area contains expected places`() {
        @Language("GraphQL")
        val fragment = """
            fragment Selection on Area {
                contains {
                    __typename 
                    ... on Area {
                        name
                    }
                    ... on Station {
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

        response.path("areaById.contains[*].name")
            .entityList<String>()
            .matches<GraphQlTester.EntityList<String>> {
                it.all { name -> name.isNotEmpty() }
            }
            .hasSizeGreaterThan(1)
    }
}
