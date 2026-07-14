package com.jeffeyalanwang.dutchrailways.backend.server.processing

import org.junit.jupiter.api.Assertions.*
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
        val stopDetails = emptyMap<String, GenericTripDetails<String>>()
        val rep = toInternalRepresentation(stopDetails)
        val range = ClosedTimeRange(parseTime("09:00"), parseTime("11:00"))

        val journeys = rRaptor(
            origin = 0,
            destination = 0,
            timeRange = range,
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertEquals(1, journeys.size)
        assertTrue(journeys[0].legs.isEmpty())
        assertEquals(0, journeys[0].finalStation)
    }

    @Test
    fun testRRaptorMultipleJourneysNoDomination() {
        val stopDetails = mapOf(
            "Trip1" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("10:30")))
            ),
            "Trip2" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("11:00"), parseTime("11:30")))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        val timeRange = ClosedTimeRange(parseTime("09:30"), parseTime("11:30"))

        val journeys = rRaptor(
            origin = amsterdamIdx,
            destination = utrechtIdx,
            timeRange = timeRange,
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        // Both Trip1 and Trip2 are within the time range, and neither dominates the other
        // Trip1: 10:00 -> 10:30
        // Trip2: 11:00 -> 11:30
        assertEquals(2, journeys.size)

        // They should be returned in chronological order of departure time
        val trip1Idx = rep.tripKey.indexOf("Trip1")
        val trip2Idx = rep.tripKey.indexOf("Trip2")

        assertEquals(trip1Idx, journeys[0].legs[0].trip)
        assertEquals(trip2Idx, journeys[1].legs[0].trip)
    }

    @Test
    fun testRRaptorDomination() {
        // Setup:
        // Trip C (Fast, early): 09:45 -> 10:15
        // Trip B (Slow, mid-early): 10:00 -> 11:00 (Dominated by Trip A and Trip C)
        // Trip A (Fast, mid-late): 10:15 -> 10:45
        // Trip D (Fast, late): 11:00 -> 11:30
        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:15"), parseTime("10:45")))
            ),
            "TripB" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("11:00")))
            ),
            "TripC" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("09:45"), parseTime("10:15")))
            ),
            "TripD" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("11:00"), parseTime("11:30")))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        val timeRange = ClosedTimeRange(parseTime("09:30"), parseTime("11:30"))

        val journeys = rRaptor(
            origin = amsterdamIdx,
            destination = utrechtIdx,
            timeRange = timeRange,
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        // TripB is dominated because:
        // - TripA departs later (10:15 vs 10:00) and arrives earlier (10:45 vs 11:00)
        // - TripC departs earlier (09:45 vs 10:00) and arrives earlier (10:15 vs 11:00)
        // Thus, TripB should be excluded.
        // Resulting list should contain TripC, TripA, TripD.
        assertEquals(3, journeys.size)

        val tripCIdx = rep.tripKey.indexOf("TripC")
        val tripAIdx = rep.tripKey.indexOf("TripA")
        val tripDIdx = rep.tripKey.indexOf("TripD")

        assertEquals(tripCIdx, journeys[0].legs[0].trip)
        assertEquals(tripAIdx, journeys[1].legs[0].trip)
        assertEquals(tripDIdx, journeys[2].legs[0].trip)
    }

    @Test
    fun testRRaptorNoJourneysInRange() {
        val stopDetails = mapOf(
            "Trip1" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("10:30")))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        // Range is before the departure time
        val timeRange = ClosedTimeRange(parseTime("08:00"), parseTime("09:30"))

        val journeys = rRaptor(
            origin = amsterdamIdx,
            destination = utrechtIdx,
            timeRange = timeRange,
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertTrue(journeys.isEmpty())
    }
}
