package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.Raptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.raptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.RouteQueryDataSource
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        val rep = RouteQueryDataSource.fromTrips<String, String>() // empty list

        // When origin == destination, should return 1 journey with empty legs
        val journeys = with(rep.transitGraph) {
            raptor(
                origin = StationId(0),
                destination = StationId(0),
                startTime = parseTime("09:00"),
            )
        }

        assertEquals(1, journeys.size)
        assertTrue(journeys[0].legs.isEmpty())
        assertEquals(0, journeys[0].finalStation.index)
    }

    @Test
    fun testRaptorDirectJourney() {
        val rep = RouteQueryDataSource.fromTrips(
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            )
        )

        val journeys = with(rep) {
            Raptor(
                origin = "Amsterdam",
                destination = "Utrecht",
                startTime = parseTime("09:50")
            )
        }

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals("Utrecht", journey.finalStation)
        assertEquals(1, journey.legs.size)
        assertEquals("Amsterdam", journey.legs[0].originStation)
        assertEquals("TripA", journey.legs[0].trip)
    }

    @Test
    fun testRaptorDirectJourneyTooLate() {

        val rep = RouteQueryDataSource.fromTrips(
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            )
        )

        // Should find no journey
        val journeys =
            with(rep) {
                Raptor(
                    origin = "Amsterdam",
                    destination = "Utrecht",
                    startTime = parseTime("10:05"),
                )
            }

        assertTrue(journeys.isEmpty())
    }

    @Test
    fun testRaptorTransferJourneySuccess() {

        val rep = RouteQueryDataSource.fromTrips(
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            ),
            "TripB" to GenericTripDetails.of(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(parseTime("10:40") to parseTime("11:10"))
            )
        )

        val journeys =
            with(rep) {
                Raptor(
                    origin = "Amsterdam",
                    destination = "Eindhoven",
                    startTime = parseTime("09:50"),
                )
            }

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals("Eindhoven", journey.finalStation)
        assertEquals(2, journey.legs.size)

        // Leg 1 (reconstructed backward: Utrecht -> Eindhoven is index 0)
        assertEquals("Utrecht", journey.legs[0].originStation)
        assertEquals("TripB", journey.legs[0].trip)

        // Leg 2 (reconstructed backward: Amsterdam -> Utrecht is index 1)
        assertEquals("Amsterdam", journey.legs[1].originStation)
        assertEquals("TripA", journey.legs[1].trip)
    }

    @Test
    fun testRaptorMissedConnection() {

        val rep = RouteQueryDataSource.fromTrips(
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            ),
            "TripB" to GenericTripDetails.of(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(parseTime("10:20") to parseTime("10:50"))
            )
        )

        val journeys =
            with(rep) {
                Raptor(
                    origin = "Amsterdam",
                    destination = "Eindhoven",
                    startTime = parseTime("09:50"),
                )
            }

        assertTrue(journeys.isEmpty())
    }

    @Test
    fun testRaptorOptimalPathSelection() {
        val rep = RouteQueryDataSource.fromTrips(
            // Route 1 (via Utrecht): Arrives Eindhoven at 11:10
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(parseTime("10:00") to parseTime("10:30"))
            ),
            "TripB" to GenericTripDetails.of(
                stations = listOf("Utrecht", "Eindhoven"),
                times = listOf(parseTime("10:40") to parseTime("11:10"))
            ),

            // Route 2 (via Rotterdam): Arrives Eindhoven at 10:55 (earlier!)
            "TripC" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Rotterdam"),
                times = listOf(parseTime("09:45") to parseTime("10:15"))
            ),
            "TripD" to GenericTripDetails.of(
                stations = listOf("Rotterdam", "Eindhoven"),
                times = listOf(parseTime("10:25") to parseTime("10:55"))
            )
        )

        val journeys =
            with(rep) {
                Raptor(
                    origin = "Amsterdam",
                    destination = "Eindhoven",
                    startTime = parseTime("09:30"),
                )
            }

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals("Eindhoven", journey.finalStation)
        assertEquals(2, journey.legs.size)

        // It should have selected the Rotterdam route (reconstructed backward: Rotterdam -> Eindhoven is index 0)
        assertEquals("Rotterdam", journey.legs[0].originStation)
        assertEquals("TripD", journey.legs[0].trip)

        // (reconstructed backward: Amsterdam -> Rotterdam is index 1)
        assertEquals("Amsterdam", journey.legs[1].originStation)
        assertEquals("TripC", journey.legs[1].trip)
    }

    @Test
    fun testRaptorMultiStop() {
        val rep = RouteQueryDataSource.fromTrips(
            // Route 1 (via Utrecht): Arrives Eindhoven at 11:10
            "TripA" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht", "Eindhoven"),
                times = listOf(
                    parseTime("10:00") to parseTime("10:30"),
                    parseTime("10:40") to parseTime("11:10"),
                )
            ),

            // Route 2 (via Rotterdam): Arrives Eindhoven at 10:55 (earlier!)
            "TripB" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Rotterdam", "Eindhoven"),
                times = listOf(
                    parseTime("09:45") to parseTime("10:15"),
                    parseTime("10:25") to parseTime("10:55"),
                )
            ),
        )

        val journeys =
            with(rep) {
                Raptor(
                    origin = "Amsterdam",
                    destination = "Eindhoven",
                    startTime = parseTime("09:30"),
                )
            }

        assertEquals(1, journeys.size)
        val journey = journeys[0]
        assertEquals("Eindhoven", journey.finalStation)
        assertEquals(2, journey.legs.size)

        // It should have selected the Rotterdam route (reconstructed backward: Rotterdam -> Eindhoven is index 0)
        assertEquals("Rotterdam", journey.legs[0].originStation)
        assertEquals("TripD", journey.legs[0].trip)

        // (reconstructed backward: Amsterdam -> Rotterdam is index 1)
        assertEquals("Amsterdam", journey.legs[1].originStation)
        assertEquals("TripC", journey.legs[1].trip)
    }
}
