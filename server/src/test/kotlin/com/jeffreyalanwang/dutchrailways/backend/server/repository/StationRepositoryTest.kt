package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.server.DutchRailwaysServerApplication
import jakarta.transaction.Transactional
import org.geolatte.geom.G2D
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*


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

    @Transactional
    @Test
    fun `Station geom processing`() {

        val stationId = 1176 // Gouda
        val position = run {
            val lon = 4.704444477700654
            val lat = 52.017501833627215
            G2D(lon, lat)
        }
        val reversedPosition = position.run { G2D(lat, lon) }

        val station = findById(stationId).get()
        assertNotEquals(reversedPosition, station.geom?.position, "`lat` and `lon` are swapped")
        assertEquals(position, station.geom?.position)

    }

    @Transactional
    @Test
    fun `Hibernate spatial search properties`() {

        val stationId = 1176 // Gouda
        val lon = 4.704444477700654
        val lat = 52.017501833627215

        val station = findById(stationId).get()

        assertNotEquals(lat, station.lon, "`lat` and `lon` are swapped")
        assertNotEquals(lon, station.lat)

        assertEquals(lat, station.lat)
        assertEquals(lon, station.lon)

    }
}