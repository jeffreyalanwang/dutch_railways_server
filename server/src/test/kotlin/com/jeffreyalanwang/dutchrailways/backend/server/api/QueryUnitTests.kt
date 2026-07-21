package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.jeffreyalanwang.dutchrailways.backend.server.dto.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.processing.JourneyFinder
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import java.time.OffsetDateTime
import kotlin.time.Clock

@GraphQlTest(GraphQlController::class)
@Import(GraphQlConfig::class, GraphQlConversionService::class)
class QueryUnitTests {

    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockkBean
    private lateinit var journeyFinder: JourneyFinder

    @Test
    fun `Query FindJourneys`() {
        val instant = Clock.System.now()

        every {
            journeyFinder.invoke(any(), any(), any(), any())
        } returns listOf(
            PointJourney.ofSingleStop(
                OffsetDateTime.now() to 0
            ),
        )

        @Language("GraphQL")
        val query = $$"""
            query FindJourneys($origin: ID!, $destination: ID!, $earliest: DateTime!) {
                findJourneys(
                    originStation: $origin,
                    destinationStation: $destination,
                    earliestDepartTime: $earliest
                ) {
                    points {
                        time,
                    }
                }
            }
        """.trimIndent()

        val response = graphQlTester.document(query)
            .variable("origin", "1")
            .variable("destination", "1")
            .variable("earliest", instant.toString())
            .execute()

        response.path("findJourneys")
            .entityList<Map<*, *>>()
            .singleElement()

        response.path("findJourneys[0].points")
            .entityList<Map<*, *>>()
            .singleElement()

        val time = response.path("findJourneys[0].points[0].time")
            .entity<OffsetDateTime>()
            .get()

        assertTrue(instant.epochSeconds - time.toEpochSecond() < 5)
    }

    @Test
    fun `Query PassServiceById`() {

        @Language("GraphQL")
        val query = $$"""
            query PassServiceById($id: ID!) {
                passServiceById(id: $id) {
                    id
                    name
                }
            }
        """.trimIndent()

        graphQlTester.document(query)
            .variable("id", "42")
            .execute()
            .path("passServiceById")
            .hasValue()
    }

    @Test
    fun `Query StationById`() {

        @Language("GraphQL")
        val query = $$"""
            query StationById($id: ID!) {
                stationById(id: $id) {
                    id
                    name
                    address
                }
            }
        """.trimIndent()

        graphQlTester.document(query)
            .variable("id", "10")
            .execute()
            .path("stationById")
            .hasValue()
    }

    @Test
    fun `Query AreaById`() {

        @Language("GraphQL")
        val query = $$"""
            query AreaById($id: ID!) {
                areaById(id: $id) {
                    id
                    name
                }
            }
        """.trimIndent()

        graphQlTester.document(query)
            .variable("id", "10")
            .execute()
            .path("areaById")
            .hasValue()
    }

    @Test
    fun `Query StopOfPassServiceAtStation`() {

        @Language("GraphQL")
        val query = $$"""
            query StopOfPassServiceAtStation($passService: ID!, $station: ID!) {
                stopOfPassServiceAtStation(passService: $passService, station: $station) {
                    arriveTime
                    departTime
                }
            }
        """.trimIndent()

        graphQlTester.document(query)
            .variable("passService", "1")
            .variable("station", "2")
            .execute()
            .run {
                path("stopOfPassServiceAtStation").hasValue()
                path("stopOfPassServiceAtStation.arriveTime").hasValue()
            }
    }
}