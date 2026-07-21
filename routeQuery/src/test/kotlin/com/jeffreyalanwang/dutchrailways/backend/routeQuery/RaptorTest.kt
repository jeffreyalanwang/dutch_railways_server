package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.Raptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun testRRaptorDoesNotExistWhenEmpty() {
        val rep = RouteQueryDataSource.fromTrips<Int, Int>() // empty

        assertThrows<IllegalArgumentException> {
            val journeys = with(rep) {
                Raptor(
                    origin = 0,
                    destination = 0,
                    startTime = parseTime("09:00"),
                )
            }
        }
    }


    @Test
    fun testRRaptorDoesNotExist() {
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

        assertThrows<IllegalArgumentException> {
            val journeys = with(rep) {
                Raptor(
                    origin = "Utrecht",
                    destination = "Rotterdam",
                    startTime = parseTime("09:00"),
                )
            }
        }

        assertThrows<IllegalArgumentException> {
            val journeys = with(rep) {
                Raptor(
                    origin = "Arnhem",
                    destination = "Utrecht",
                    startTime = parseTime("09:00"),
                )
            }
        }
    }

    @Test
    fun testRaptorSameOriginAndDestination() {
        val rep = RouteQueryDataSource.fromTrips(
            "Trip1" to GenericTripDetails.of(
                stations = listOf("Amsterdam", "Utrecht"),
                times = listOf(Pair(parseTime("10:00"), parseTime("10:30")))
            ),
        )

        val journeys = with(rep) {
            Raptor(
                origin = "Utrecht",
                destination = "Utrecht",
                startTime = parseTime("11:00"),
            )
        }

        assertEquals(1, journeys.size)
        assertTrue(journeys[0].legStartPoints.isEmpty())
        assertEquals("Utrecht", journeys[0].finalStation)
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
        assertEquals(1, journey.legStartPoints.size)
        assertEquals("Amsterdam", journey.legStartPoints[0].originStation)
        assertEquals("TripA", journey.legStartPoints[0].trip)
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

        val journey = journeys.single()

        assertEquals(2, journey.legStartPoints.size)

        with (journey.legStartPoints[0]) {
            assertEquals("Amsterdam", originStation)
            assertEquals("TripA", trip)
        }
        with (journey.legStartPoints[1]) {
            assertEquals("Utrecht", originStation)
            assertEquals("TripB", trip)
        }
        assertEquals("Eindhoven", journey.finalStation)
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

        val journey = journeys.single()

        assertEquals(2, journey.legStartPoints.size)

        // It should have selected the Rotterdam route
        with (journey.legStartPoints[0]) {
            assertEquals("Amsterdam", originStation)
            assertEquals("TripC", trip)
        }
        with (journey.legStartPoints[1]) {
            assertEquals("Rotterdam", originStation)
            assertEquals("TripD", trip)
        }
        assertEquals("Eindhoven", journey.finalStation)
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
        assertEquals("Amsterdam", journey.legStartPoints[0].originStation)
        assertEquals("Eindhoven", journey.finalStation)
        assertEquals(1, journey.legStartPoints.size)
        assertEquals("TripB", journey.legStartPoints[0].trip)

    }
}
