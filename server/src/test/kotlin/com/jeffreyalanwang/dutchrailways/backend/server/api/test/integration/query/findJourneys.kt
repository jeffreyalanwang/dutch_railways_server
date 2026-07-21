package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class `Integration test for findJourneys query`(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
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
    val earliest = LocalDate.orNull(2026, 5, 1)!!.atStartOfDayIn(TimeZone.UTC)
    val latest = LocalDate.orNull(2026, 6, 1)!!.atStartOfDayIn(TimeZone.UTC)

    val argMap = mapOf(
        "origin" to origin,
        "destination" to destination,
        "earliest" to earliest.toString(),
        "latest" to latest.toString(),
    )

    @Test
    fun `Returns at least one solution`() {

        @Language("GraphQL")
        val fragment = $$"""
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
    fun `Journey visits at least two stations`() {

        @Language("GraphQL")
        val fragment = $$"""
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
        val fragment = $$"""
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
            .matches { it.toInstant().toKotlinInstant() > earliest }

        response.path("findJourneys[0].points[-1].time").entity<OffsetDateTime>()
            .matches { it.toInstant().toKotlinInstant() < latest }
    }

    @Test
    fun `Journey made it to intended endpoints`() {

        @Language("GraphQL")
        val fragment = $$"""
            fragment Selection on Journey {
                points {
                    time,
                    place { 
                        id,
                    },
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

        response.path("findJourneys[0].points[0].place.id").entity<Int>()
            .equals(origin)

        response.path("findJourneys[0].points[-1].place.id").entity<Int>()
            .equals(destination)
    }

    @Test
    fun `Journey returns 'via' field`() {

        @Language("GraphQL")
        val fragment = $$"""
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

        response.path("findJourneys[0].points[0].via.id").hasValue()
    }
}