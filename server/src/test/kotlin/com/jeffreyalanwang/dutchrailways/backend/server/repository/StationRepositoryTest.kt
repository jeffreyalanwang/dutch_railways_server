package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.server.DutchRailwaysServerApplication
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import io.mockk.every
import io.mockk.spyk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Uses the same database as configured for production for convenience.
 */
@SpringBootTest
@ContextConfiguration(classes = [DutchRailwaysServerApplication::class])
class StationRepositoryTest(
    @Autowired val stationRepository: StationRepository,
) {
    @Test
    fun `getAllStationIds() returns a set of unique IDs`() {
        val results = stationRepository.getAllStationIds()
        assertFalse(results.isEmpty())
        assertEquals(results.size, results.distinct().size)
    }

    @Test
    fun `getStationById() returns station with requested ID`() {
        val stationId = 1176
        val result = stationRepository.getStationById(stationId)
        assertEquals(stationId, result.id)
    }

    @Test
    fun `getStopsDepartingAfter() returns a requested number of stops from a station`() {
        val instant = Clock.System.now() - (100 * 365.days)
        val stationId = 1176

        val results = stationRepository.getStopsDepartingAfter(instant, stationId = stationId, count = 1)

        assertTrue(
            results.all { it.stationId == stationId }
        )
        assertTrue(
            results.all { instant < it.departTime!!.toInstant().toKotlinInstant() }
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `atOffsetOf() sets the timezone for an instant (mocked database)`() {
        val stationRepository = spyk(stationRepository)

        val instant = Clock.System.now()
        val stationId = 1176

        every {
            stationRepository.getStopsDepartingAfter(any<Instant>(), stationId = any(), count = 1)
        } returns listOf(
            Stop(
                serviceId = 0,
                arriveTime = OffsetDateTime.now() - 20.minutes.toJavaDuration(),
                departTime = OffsetDateTime.now() - 10.minutes.toJavaDuration(),
                stationId = 0,
            ),
        )

        val offsetDT = with(stationRepository) {
            instant.atOffsetOf(stationId)
        }

        assertEquals(instant, offsetDT.toInstant().toKotlinInstant(), "Returned value did not change absolute time point")
        assertEquals(OffsetDateTime.now().offset, offsetDT.offset, "Returned value has the expected zone offset")
    }

    @Test
    fun `atOffsetOf() sets the timezone for an instant`() {
        // TODO currently the database does not support timezones, so times are all assumed to be UTC.
        val instant = Clock.System.now()
        val stationId = 1176

        val offsetDT = with(stationRepository) {
            instant.atOffsetOf(stationId)
        }

        assertEquals(instant, offsetDT.toInstant().toKotlinInstant(), "Returned value did not change absolute time point")
        assertEquals(ZoneOffset.UTC, offsetDT.offset, "Returned value has the expected zone offset")
    }
}