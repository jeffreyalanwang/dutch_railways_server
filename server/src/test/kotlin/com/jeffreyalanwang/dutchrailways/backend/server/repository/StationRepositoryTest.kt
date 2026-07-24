package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.server.DutchRailwaysServerApplication
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Uses the same database as configured for production for convenience.
 */
@SpringBootTest
@ContextConfiguration(classes = [DutchRailwaysServerApplication::class])
class StationRepositoryTest(
    @Autowired val stationRepository: StationRepository,
): StationRepository by stationRepository {
    @Test
    fun `getAllStationIds() returns a set of unique IDs`() {
        val results = getAllStationIds()
        assertFalse(results.isEmpty())
        assertEquals(results.size, results.distinct().size)
    }

    @Test
    fun `getStops(stationId, arriveOrDepartAfter, count) returns a requested number of stops from a station`() {
        val instant = Instant.now() - Duration.ofDays(100 * 365)
        val stationId = 1176

        val results = getStops(stationId, arriveOrDepartAfter = instant, count = 1)

        assertTrue(
            results.all { it.stationId == stationId }
        )
        assertTrue(
            results.all { instant < it.departTime!! }
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `atOffsetOf() sets the timezone for an instant`() {

        val instant = Instant.now()
        val stationId = 1176

        val offsetDT = instant.atOffsetIn(stationId)

        assertEquals(instant, offsetDT.toInstant(), "Returned value should not change absolute time point")
        assertEquals(instant.atZone(ZoneId.of("Europe/Amsterdam")).offset, offsetDT.offset, "Returned value has the expected zone offset")
    }

    @Transactional
    @Test
    fun `Station entity object fields`() {

        val stationId = 1176
        val station = findById(stationId).get()
        with (station) {
            assertFalse(stops.isNullOrEmpty())
            assertFalse(address.isBlank())
            assertFalse(geom?.isEmpty ?: false)
        }

    }
}