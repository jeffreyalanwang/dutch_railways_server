package com.jeffreyalanwang.dutchrailways.backend.server.api.test.unit.controller.mutation

import com.jeffreyalanwang.dutchrailways.backend.server.api.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.api.PassServiceMutationController
import com.jeffreyalanwang.dutchrailways.backend.server.api.PassServiceQueryController
import com.jeffreyalanwang.dutchrailways.backend.server.dto.AmenityEnum
import com.jeffreyalanwang.dutchrailways.backend.server.dto.TrainsetTypeEnum
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Amenity
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop.Comparators.byArriveTime
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.TrainsetType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.OffsetDateTime

@GraphQlTest(PassServiceMutationController::class, PassServiceQueryController::class)
@Import(GraphQlConfig::class)
class PassServiceMutationUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkBean private lateinit var passServiceRepository: PassServiceRepository

    val stationIds = listOf(0, 1, 2)
    val times = generateSequence( OffsetDateTime.now() ) { prev -> prev.plusMinutes(1) }
        .chunked(size = 2) { it.first() to it.last() }
        .take(stationIds.size).toList()
        .run {
            listOf(null to first().second) +
            drop(1).dropLast(1) +
            listOf(last().first to null)
        }

    val name = "Sprinter 101"
    val trainset = "SLT"
    val amenities = listOf("WIFI")
    val stops = stationIds.indices.map { i ->
        mapOf(
            "station" to stationIds[i],
            "arriveTime" to times[i].first,
            "departTime" to times[i].second,
        )
    }

    val passServiceMap = mapOf(
        "name" to name,
        "trainset" to trainset,
        "amenities" to amenities,
        "stops" to stops
    )

    @BeforeEach
    fun setUp() {
        every {
            passServiceRepository.save(any())
        } answers {
            firstArg()
        }

        every {
            passServiceRepository.getTrainsetEntity(any())
        } answers {
            TrainsetType(name = firstArg<TrainsetTypeEnum>().name, amenities = mutableSetOf())
        }

        every {
            passServiceRepository.getAmenityEntity(any())
        } answers {
            firstArg<Collection<AmenityEnum>>().map {
                Amenity(id = 0, description = it.name)
            }
        }
    }

    @Test
    fun `Create PassService`() {
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

        verify { passServiceRepository.save(withArg {
            assertEquals(name, it.name)
            assertEquals(amenities.toSet(), it.consist!!.amenities.map { it.description }.toSet())
            assertEquals(stops, it.stops.sortedWith(byArriveTime).map {
                mapOf(
                    "station" to it.stationId,
                    "arriveTime" to it.arriveTime,
                    "departTime" to it.departTime,
                )
            })
        }) }

        response
            .path("createPassService").hasValue()
            .path("createPassService.id").hasValue()
    }

    @Test
    fun `Update PassService`() = TODO() as Unit

    @Test
    fun `Delete PassService`() = TODO() as Unit
}