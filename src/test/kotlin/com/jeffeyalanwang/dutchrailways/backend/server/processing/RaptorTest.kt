package com.jeffeyalanwang.dutchrailways.backend.server.processing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class RaptorTest {

    private fun parseTime(time: String): Instant {
        val parts = time.split(":")
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val totalMinutes = hours * 60 + minutes
        return Instant.fromEpochMilliseconds(totalMinutes * 60 * 1000)
    }

    @Test
    fun testRaptorSameOriginAndDestination() {
        val stopDetails = emptyMap<String, GenericTripDetails<String>>()
        val rep = toInternalRepresentation(stopDetails)
        val stations = rep.internalStations
        val trips = rep.internalTrips

        // When origin == destination, should return 1 journey with empty legs
        val journeys = raptor(
            origin = 0,
            destination = 0,
            startTime = parseTime("09:00"),
            trips = trips,
            stations = stations
        )

        assertEquals(1, journeys.size)
        assertTrue(journeys[0].legs.isEmpty())
        assertEquals(0, journeys[0].finalStation)
    }

    @Test
    fun testRaptorDirectJourney() {
        val t1000 = parseTime("10:00")
        val t1030 = parseTime("10:30")

        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(t1000, t1030))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        // Query before departure
        val journeys = raptor(
            origin = amsterdamIdx,
            destination = utrechtIdx,
            startTime = parseTime("09:50"),
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals(utrechtIdx, journey.finalStation)
        assertEquals(1, journey.legs.size)
        assertEquals(amsterdamIdx, journey.legs[0].originStation)
        assertEquals(rep.tripKey.indexOf("TripA"), journey.legs[0].trip)
    }

    @Test
    fun testRaptorDirectJourneyTooLate() {
        val t1000 = parseTime("10:00")
        val t1030 = parseTime("10:30")

        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(t1000, t1030))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        // Query after departure: should find no journey
        val journeys = raptor(
            origin = amsterdamIdx,
            destination = utrechtIdx,
            startTime = parseTime("10:05"),
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertTrue(journeys.isEmpty())
    }

    @Test
    fun testRaptorTransferJourneySuccess() {
        val t1000 = parseTime("10:00")
        val t1030 = parseTime("10:30")
        val t1040 = parseTime("10:40")
        val t1110 = parseTime("11:10")

        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(t1000, t1030))
            ),
            "TripB" to GenericTripDetails(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(Pair(t1040, t1110))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val eindhovenIdx = rep.stationKey.indexOf("Eindhoven")
        val utrechtIdx = rep.stationKey.indexOf("Utrecht")

        val journeys = raptor(
            origin = amsterdamIdx,
            destination = eindhovenIdx,
            startTime = parseTime("09:50"),
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals(eindhovenIdx, journey.finalStation)
        assertEquals(2, journey.legs.size)

        // Leg 1 (reconstructed backward: Utrecht -> Eindhoven is index 0)
        assertEquals(utrechtIdx, journey.legs[0].originStation)
        assertEquals(rep.tripKey.indexOf("TripB"), journey.legs[0].trip)

        // Leg 2 (reconstructed backward: Amsterdam -> Utrecht is index 1)
        assertEquals(amsterdamIdx, journey.legs[1].originStation)
        assertEquals(rep.tripKey.indexOf("TripA"), journey.legs[1].trip)
    }

    @Test
    fun testRaptorMissedConnection() {
        val t1000 = parseTime("10:00")
        val t1030 = parseTime("10:30")
        val t1020 = parseTime("10:20") // departs before arrival of TripA
        val t1050 = parseTime("10:50")

        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(t1000, t1030))
            ),
            "TripB" to GenericTripDetails(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(Pair(t1020, t1050))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val eindhovenIdx = rep.stationKey.indexOf("Eindhoven")

        val journeys = raptor(
            origin = amsterdamIdx,
            destination = eindhovenIdx,
            startTime = parseTime("09:50"),
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertTrue(journeys.isEmpty())
    }

    @Test
    fun testRaptorOptimalPathSelection() {
        // Route 1 (via Utrecht): Arrives Eindhoven at 11:10
        val t1000 = parseTime("10:00")
        val t1030 = parseTime("10:30")
        val t1040 = parseTime("10:40")
        val t1110 = parseTime("11:10")

        // Route 2 (via Rotterdam): Arrives Eindhoven at 10:55 (earlier!)
        val t0945 = parseTime("09:45")
        val t1015 = parseTime("10:15")
        val t1025 = parseTime("10:25")
        val t1055 = parseTime("10:55")

        val stopDetails = mapOf(
            "TripA" to GenericTripDetails(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(t1000, t1030))
            ),
            "TripB" to GenericTripDetails(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(Pair(t1040, t1110))
            ),
            "TripC" to GenericTripDetails(
                stations = listOf("Amsterdam", "Rotterdam"),
                times = listOf(Pair(t0945, t1015))
            ),
            "TripD" to GenericTripDetails(
                stations = listOf("Rotterdam", "Eindhoven"),
                times = listOf(Pair(t1025, t1055))
            )
        )
        val rep = toInternalRepresentation(stopDetails)

        val amsterdamIdx = rep.stationKey.indexOf("Amsterdam")
        val eindhovenIdx = rep.stationKey.indexOf("Eindhoven")
        val rotterdamIdx = rep.stationKey.indexOf("Rotterdam")

        val journeys = raptor(
            origin = amsterdamIdx,
            destination = eindhovenIdx,
            startTime = parseTime("09:30"),
            trips = rep.internalTrips,
            stations = rep.internalStations
        )

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals(eindhovenIdx, journey.finalStation)
        assertEquals(2, journey.legs.size)

        // It should have selected the Rotterdam route (reconstructed backward: Rotterdam -> Eindhoven is index 0)
        assertEquals(rotterdamIdx, journey.legs[0].originStation)
        assertEquals(rep.tripKey.indexOf("TripD"), journey.legs[0].trip)

        // (reconstructed backward: Amsterdam -> Rotterdam is index 1)
        assertEquals(amsterdamIdx, journey.legs[1].originStation)
        assertEquals(rep.tripKey.indexOf("TripC"), journey.legs[1].trip)
    }
}
