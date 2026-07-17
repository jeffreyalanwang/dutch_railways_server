package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.RRaptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.RouteQueryDataSource
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class RRaptorTest {

    private fun parseTime(time: String): Instant {
        val parts = time.split(":")
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val totalMinutes = hours * 60 + minutes
        return Instant.fromEpochMilliseconds(totalMinutes * 60 * 1000)
    }

    @Test
    fun testRRaptorSameOriginAndDestination() {
        val rep = RouteQueryDataSource.fromTrips<Int, Int>() // empty

        val journeys = with(rep) {
            RRaptor(
                origin = 0,
                destination = 0,
                timeRange = parseTime("09:00")..parseTime("11:00"),
            )
        }

        assertEquals(1, journeys.size)
        assertTrue(journeys[0].legs.isEmpty())
        assertEquals(0, journeys[0].finalStation)
    }

    @Test
    fun testRRaptorMultipleJourneysNoDomination() {
        val rep = RouteQueryDataSource.fromTrips(
            "Trip1" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("10:30")))
            ),
            "Trip2" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("11:00"), parseTime("11:30")))
            ),
        )

        val journeys = with(rep) {
            RRaptor(
                origin = "Amsterdam",
                destination = "Utrecht",
                timeRange = parseTime("09:30")..parseTime("11:30"),
            )
        }

        // Both Trip1 and Trip2 are within the time range, and neither dominates the other
        // Trip1: 10:00 -> 10:30
        // Trip2: 11:00 -> 11:30
        assertEquals(2, journeys.size)

        // They should be returned in chronological order of departure time

        assertEquals("Trip1", journeys[0].legs[0].trip)
        assertEquals("Trip2", journeys[1].legs[0].trip)
    }

    @Test
    fun testRRaptorDomination() {
        // Setup:
        // Trip C (Fast, early): 09:45 -> 10:15
        // Trip B (Slow, mid-early): 10:00 -> 11:00 (Dominated by Trip A and Trip C)
        // Trip A (Fast, mid-late): 10:15 -> 10:45
        // Trip D (Fast, late): 11:00 -> 11:30
        val rep = RouteQueryDataSource.fromTrips(
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:15"), parseTime("10:45")))
            ),
            "TripB" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("11:00")))
            ),
            "TripC" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("09:45"), parseTime("10:15")))
            ),
            "TripD" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("11:00"), parseTime("11:30")))
            )
        )

        val journeys = with(rep) {
            RRaptor(
                origin = "Amsterdam",
                destination = "Utrecht",
                timeRange = parseTime("09:30")..parseTime("11:30"),
            )
        }

        // TripB is dominated because:
        // - TripA departs later (10:15 vs 10:00) and arrives earlier (10:45 vs 11:00)
        // - TripC departs earlier (09:45 vs 10:00) and arrives earlier (10:15 vs 11:00)
        // Thus, TripB should be excluded.
        // Resulting list should contain TripC, TripA, TripD.
        assertEquals(3, journeys.size)

        assertEquals("TripC", journeys[0].legs[0].trip)
        assertEquals("TripA", journeys[1].legs[0].trip)
        assertEquals("TripD", journeys[2].legs[0].trip)
    }

    @Test
    fun testRRaptorNoJourneysInRange() {
        val rep = RouteQueryDataSource.fromTrips(
            "Trip1" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            )
        )

        // Range is before the departure time
        val timeRange = parseTime("08:00")..parseTime("09:30")

        val journeys = with(rep) {
            RRaptor(
                origin = "Amsterdam",
                destination = "Utrecht",
                timeRange = timeRange,
            )
        }

        assertTrue(journeys.isEmpty())
    }
}
