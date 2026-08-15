package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.database.testing.SampleDatabaseTest
import com.jeffreyalanwang.dutchrailways.api.Trainset
import com.jeffreyalanwang.dutchrailways.backend.server.repository.SetCompareBuilderScope.Companion.allSetEqual
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.jeffreyalanwang.dutchrailways.api.Amenity as AmenityEnum

/**
 * Uses the same database as configured for production for convenience.
 */
@SpringBootTest
@SampleDatabaseTest
class PassServiceRepositoryTest(
    @Autowired val passServiceRepository: PassServiceRepository,
) {
    @Test
    fun `getStops(serviceIdAndStationId) returns values in same order as input`() {
        val serviceToStation = listOf(
            651 to 1176,
            651 to 1164,
            651 to 1156,
            574 to 1198,
            574 to 1176,
            574 to 1156,
        )

        val stops = passServiceRepository.getStop(serviceToStation)

        assertEquals(serviceToStation.size, stops.size)

        serviceToStation.zip(stops) { (expectedServiceId, expectedStationId), stop ->
            assertEquals(expectedServiceId, stop.serviceId)
            assertEquals(expectedStationId, stop.stationId)
        }
    }

    @Test
    fun `getAllTimetables() returns collection of valid shape`() {
        val timetables = passServiceRepository.getAllTimetables()

        with (timetables) {
            assertEquals(size, distinctBy { it.id }.size, "Output value should have a unique set of keys")
            forEach {
                assertTrue("A PassService should have more than one stop in its timetable") { it.stops.size >= 2 }
            }
        }
    }

    @Test
    fun `getAmenityEntity()`() {
        val enums = AmenityEnum.entries
        val entities = passServiceRepository.getAmenityEntity(enums)
        assertTrue(
            allSetEqual {
                enums on { it.name }
                entities on { it.description }
            }
        )
    }

    @ParameterizedTest
    @EnumSource(Trainset::class)
    fun `getTrainsetEntity()`(enum: Trainset) {
        val entity = passServiceRepository.getTrainsetEntity(enum)
        assertEquals(enum.name, entity.name)
    }
}