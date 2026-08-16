package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.database.testing.SampleDatabaseTest
import com.jeffreyalanwang.util.geolatte.crs.assertNear
import jakarta.transaction.Transactional
import org.geolatte.geom.G2D
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

/**
 * Uses the same database as configured for production for convenience.
 */
@SpringBootTest
@SampleDatabaseTest
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
    fun `getStops(stationId, arriveOrDepartAfter, count) returns a requested number of stops from a station`() {
        val instant = Instant.now() - Duration.ofDays(100 * 365)
        val stationId = 1176

        val results = stationRepository.getStops(stationId, arriveOrDepartAfter = instant, count = 1)

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

        val offsetDT = stationRepository.run { instant.atOffsetIn(stationId) }

        assertEquals(instant, offsetDT.toInstant(), "Returned value should not change absolute time point")
        assertEquals(instant.atZone(ZoneId.of("Europe/Amsterdam")).offset, offsetDT.offset, "Returned value has the expected zone offset")
    }

    @Transactional
    @Test
    fun `Station entity object fields`() {

        val stationId = 1176
        val station = stationRepository.findById(stationId).get()
        with (station) {
            assertFalse(stops.isNullOrEmpty())
            assertFalse(address.isBlank())
            assertFalse(geom?.isEmpty ?: false)
        }

    }

    @Transactional
    @ParameterizedTest
    @CsvSource(
        "1176, 4.704444477700654, 52.017501833627215", // Station Gouda
        "1155, 5.481389125142196, 51.443332672028480", // Eindhoven Centraal
    )
    fun `Station geom processing`(stationId: Int, lon: Double, lat: Double) {

        val position = G2D(lon, lat)
        val reversedPosition = G2D(lat, lon)

        val station = stationRepository.findById(stationId).get()
        assertNotEquals(reversedPosition, station.geom?.position, "`lat` and `lon` are swapped")
        assertNear(position, station.geom?.position!!)

    }

    @Transactional
    @ParameterizedTest
    @CsvSource(
        "1176, 4.704444477700654, 52.017501833627215", // Station Gouda
        "1155, 5.481389125142196, 51.443332672028480", // Eindhoven Centraal
    )
    fun `Hibernate spatial search properties`(stationId: Int, lon: Double, lat: Double) {

        val station = stationRepository.findById(stationId).get()

        assertNotEquals(lat, station.lon, "`lat` and `lon` are swapped")
        assertNotEquals(lon, station.lat)

        assertNear(lat, station.lat!!)
        assertNear(lon, station.lon!!)

    }
}