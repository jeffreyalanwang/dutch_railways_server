package com.jeffreyalanwang.dutchrailways.backend.server.integrationTest

import com.jeffreyalanwang.dutchrailways.backend.database.testing.SampleDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@AutoConfigureHttpGraphQlTester
@SpringBootTest
@SampleDatabaseTest
class QueryFindJourneysIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
    @Suppress("GraphQLUnresolvedReference")
    @Language("GraphQL")
    val query = $$"""
        query FindJourneys($origin: ID!, $destination: ID!, $earliest: DateTime!, $latest: DateTime) {
            findJourneys(
                originStation: $origin,
                destinationStation: $destination,
                earliestDepartTime: $earliest,
                latestArriveTime: $latest,
            ) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    val origin = 1176
    val destination = 1247
    val earliest = LocalDate.of(2026, 5, 1).atStartOfDay().atOffset(ZoneOffset.UTC)
    val latest = LocalDate.of(2026, 6, 1).atStartOfDay().atOffset(ZoneOffset.UTC)

    val argMap = mapOf(
        "origin" to origin,
        "destination" to destination,
        "earliest" to earliest.toString(),
        "latest" to latest.toString(),
    )

    @Test
    fun `Returns at least one solution`() {

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    time
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("findJourneys").entityList<Any>()
            .hasSizeGreaterThan(0)
    }

    @Test
    fun `Journey includes at least two points`() {

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    time
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("findJourneys[0].points").entityList<Map<String, Any>>()
            .hasSizeGreaterThan(1)
    }

    @Test
    fun `Journey respects time boundaries`() {

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    time
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("findJourneys[0].points[0].time").entity<OffsetDateTime>()
            .matches { it > earliest }

        response.path("findJourneys[0].points[-1].time").entity<OffsetDateTime>()
            .matches { it < latest }
    }

    @Test
    fun `Journey made it to intended endpoints`() {

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    time,
                    place { 
                        id,
                    },
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("findJourneys[0].points[0].place.id").entity<Int>()
            .equals(origin)

        response.path("findJourneys[0].points[-1].place.id").entity<Int>()
            .equals(destination)
    }

    @Test
    fun `Journey returns 'via' field`() {

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    via {
                        id,
                    }
                }
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variables(argMap)
            .fragment(fragment)
            .execute()

        response.path("findJourneys[0]").hasValue()
        response.path("findJourneys[0].points[0]").hasValue()
        response.path("findJourneys[0].points[0].via").hasValue()
        response.path("findJourneys[0].points[0].via.id").hasValue()
    }
}