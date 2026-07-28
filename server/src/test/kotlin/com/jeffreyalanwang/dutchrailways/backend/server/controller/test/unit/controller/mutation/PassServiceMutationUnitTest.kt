package com.jeffreyalanwang.dutchrailways.backend.server.controller.test.unit.controller.mutation

import com.jeffreyalanwang.dutchrailways.backend.server.controller.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.controller.mutation.PassServiceMutationController
import com.jeffreyalanwang.dutchrailways.backend.server.controller.query.PassServiceQueryController
import com.jeffreyalanwang.dutchrailways.api.Trainset
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.SetCompareBuilderScope.Companion.allSetEqual
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop.Comparators.byArriveTime
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.TrainsetType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import java.time.OffsetDateTime
import java.util.*
import com.jeffreyalanwang.dutchrailways.api.Amenity as AmenityEnum
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Amenity as AmenityEntity

@GraphQlTest(PassServiceMutationController::class, PassServiceQueryController::class)
@Import(GraphQlConfig::class)
class PassServiceMutationUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkBean private lateinit var passServiceRepository: PassServiceRepository

    val name = "Sprinter 101"
    val trainset = "SLT"
    val amenities = listOf("WIFI")

    val stationIds = listOf(0, 1, 2)
    val times = generateSequence( OffsetDateTime.now() ) { prev -> prev.plusMinutes(1) }
        .chunked(size = 2) { it.first() to it.last() }
        .take(stationIds.size).toList()
        .run {
            listOf(null to first().second) +
                    drop(1).dropLast(1) +
                    listOf(last().first to null)
        }

    val passServiceMap = mapOf(
        "name" to name,
        "trainset" to trainset,
        "amenities" to amenities,
        "stops" to stationIds.indices.map { i ->
            mapOf(
                "station" to stationIds[i],
                "arriveTime" to times[i].first,
                "departTime" to times[i].second,
            )
        }
    )

    @BeforeEach
    fun setUp() {
        every { passServiceRepository.save(any()) } answers { firstArg() }
        justRun { passServiceRepository.deleteById(any()) }

        every {
            passServiceRepository.getTrainsetEntity(any())
        } answers {
            TrainsetType(name = firstArg<Trainset>().name, amenities = mutableSetOf())
        }

        every {
            passServiceRepository.getAmenityEntity(any())
        } answers {
            firstArg<Collection<AmenityEnum>>().map {
                AmenityEntity(id = 0, description = it.name)
            }
        }
    }

    @Test
    fun `Create PassService`() {
        @Language("GraphQL")
        val query = $$"""
            mutation CreatePassService($details: MutationPassService!) {
                createPassService(details: $details) {
                    id
                }
            }
        """.trimIndent()

        val response = graphQlTester.document(query)
            .variable("details", passServiceMap)
            .execute()

        verify { passServiceRepository.save(
            withArg {
                assertEquals(name, it.name)
                assertEquals(amenities.toSet(), it.consist!!.amenities.map { it.description }.toSet())

                (stationIds zip times).zip(it.stops.sortedWith(byArriveTime)) { (expectedStationId, expectedTimes), actualStop ->
                    assertEquals(expectedStationId, actualStop.stationId)
                    assertEquals(expectedTimes.component1()?.toInstant(), actualStop.component1())
                    assertEquals(expectedTimes.component2()?.toInstant(), actualStop.component2())
                }
            }
        ) }

        response
            .path("createPassService").hasValue()
            .path("createPassService.id").hasValue()
    }

    @Test
    fun `Update PassService`() {

        val id = 5

        @Language("GraphQL")
        val query = $$"""
            mutation UpdatePassService($id: ID!, $details: MutationPassService!) {
                updatePassService(id: $id details: $details) {
                    id
                    name
                    trainset
                    amenities
                }
            }
        """.trimIndent()

        every {
            passServiceRepository.findById(any())
        } returns Optional.of(
            PassService(
                id = id,
                name = "Old Name",
            ).apply {
                consist = null
                stops.clear()
            }
        )

        val response = graphQlTester.document(query)
            .variable("id", id)
            .variable("details", passServiceMap)
            .execute()

        verify {
            passServiceRepository.findById(id)
            passServiceRepository.save(
                withArg {
                    assertEquals(name, it.name)
                    assertEquals(trainset, it.consist!!.name)

                    // Compare amenities
                    assertTrue(
                        allSetEqual {
                            amenities on itself
                            it.consist!!.amenities on { it.description }
                        }
                    )

                    // Compare stops
                    (stationIds zip times).zip(it.stops.sortedWith(byArriveTime)) { (expectedStationId, expectedTimes), actualStop ->
                        assertEquals(id, actualStop.serviceId)
                        assertEquals(expectedStationId, actualStop.stationId)

                        // Compare arrival, then departure time
                        assertEquals(expectedTimes.component1()?.toInstant(), actualStop.component1())
                        assertEquals(expectedTimes.component2()?.toInstant(), actualStop.component2())
                    }
                }
            )
        }

        response
            .path("updatePassService").hasValue()
            .path("updatePassService.id").entity<Int>().isEqualTo(id)
            .path("updatePassService.amenities[*]")
                .entityList<String>()
                .containsExactly(*amenities.toTypedArray())
    }

    @Test
    fun `Delete PassService`() {

        val id = 0

        @Language("GraphQL")
        val query = $$"""
            mutation DeletePassService($id: ID!) {
                deletePassService(id: $id)
            }
        """.trimIndent()

        every { passServiceRepository.existsById(any()) } returns true

        val response = graphQlTester.document(query)
            .variable("id", id)
            .execute()

        verify { passServiceRepository.deleteById(id) }

        response.path("deletePassService").entity<Int>().isEqualTo(id)

    }

    @Test
    fun `Delete PassService (did not exist)`() {

        val id = 0

        @Language("GraphQL")
        val query = $$"""
            mutation DeletePassService($id: ID!) {
                deletePassService(id: $id)
            }
        """.trimIndent()

        every { passServiceRepository.existsById(any()) } returns false

        val response = graphQlTester.document(query)
            .variable("id", id)
            .execute()

        verify(exactly = 0) { passServiceRepository.deleteById(id) }

        response.path("deletePassService").valueIsNull()

    }
}