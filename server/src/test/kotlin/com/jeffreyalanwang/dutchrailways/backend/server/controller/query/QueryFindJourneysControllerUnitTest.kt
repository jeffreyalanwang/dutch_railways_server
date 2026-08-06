package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.controller.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.processing.journey.JourneyFinder
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.MINUTES

@GraphQlTest(JourneyQueryController::class, PassServiceQueryController::class)
@Import(GraphQlConfig::class)
class QueryFindJourneysControllerUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkSpyBean private lateinit var journeyController: JourneyQueryController
    @MockkSpyBean private lateinit var passServiceController: PassServiceQueryController

    @MockkBean private lateinit var journeyFinder: JourneyFinder
    @MockkBean private lateinit var passServiceRepository: PassServiceRepository

    @Suppress("GraphQLUnresolvedReference")
    @Language("GraphQL")
    val rangeQuery = $$"""
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

    @Test
    fun `Journey returns 'time' field`() {

        val origin = 1
        val destination = 1
        val instant = Instant.now()

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Journey {
                points {
                    time
                }
            }
        """.trimIndent()

        every {
            journeyFinder.invoke(any(), any(), any(), any())
        } answers {
            listOf(
                PointJourney.ofSingleStop(OffsetDateTime.now(), place = 0)
            )
        }

        val response = graphQlTester.document(rangeQuery)
            .variable("origin", origin)
            .variable("destination", destination)
            .variable("earliest", instant.toString())
            .variable("earliest", instant.plus(5, MINUTES).toString())
            .fragment(fragment)
            .execute()

        verify {
            journeyController.findJourneys(any(), any(), any(), any())
            journeyFinder.invoke(any(), any(), any(), any())
        }

        response.path("findJourneys").entityList<Map<*, *>>().singleElement()
        response.path("findJourneys[0].points").entityList<Map<*, *>>().singleElement()

        val time = response.path("findJourneys[0].points[0].time")
            .entity<OffsetDateTime>()
            .get()

        assertTrue(instant.epochSecond - time.toEpochSecond() < 5)
    }

    @Test
    fun `Journey returns 'via' field`() {

        val origin = 1176
        val destination = 1247
        val earliest = LocalDate.of(2026, 5, 1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val latest = LocalDate.of(2026, 6, 1).atStartOfDay().atOffset(ZoneOffset.UTC)

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

        every {
            journeyFinder.invoke(any(), any(), any(), any())
        } returns listOf(
            PointJourney
                departingAt earliest.plusMinutes( 0) fromStation origin
                viaPassService 5
                arrivingAt  earliest.plusMinutes(30) atStation destination
        )

        every {
            passServiceRepository.findAllById(any())
        } answers {
            firstArg<Iterable<Int>>().map { id -> PassService(id = id, name = "") }
        }

        val response = graphQlTester
            .document(rangeQuery)
            .variable("origin", origin)
            .variable("destination", destination)
            .variable("earliest", earliest.toString())
            .variable("latest", latest.toString())
            .fragment(fragment)
            .execute()

        verify {
            journeyController.findJourneys(
                origin,
                destination,
                earliest.toInstant(),
                latest.toInstant(),
            )

            passServiceController.run {
                any<PointJourney.JourneyPoint>().via(any())
            }
        }

        response.path("findJourneys[0]").hasValue()
        response.path("findJourneys[0].points[0]").hasValue()
        response.path("findJourneys[0].points[0].via").hasValue()
        response.path("findJourneys[0].points[0].via.id").entity<Int>().isEqualTo(5)
    }
}