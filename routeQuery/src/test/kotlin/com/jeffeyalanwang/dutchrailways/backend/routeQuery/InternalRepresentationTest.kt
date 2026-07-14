package com.jeffeyalanwang.dutchrailways.backend.routeQuery

import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal.Trip
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.toInternalRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class InternalRepresentationTest {

    @Test
    fun testInternalRepresentationMapping() {
        // Prepare some sample stations and times
        val t0 = Instant.fromEpochMilliseconds(1000)
        val t1 = Instant.fromEpochMilliseconds(2000)
        val t2 = Instant.fromEpochMilliseconds(3000)
        val t3 = Instant.fromEpochMilliseconds(4000)

        // Trip 1: "Amsterdam" -> "Utrecht" -> "Eindhoven"
        val trip1Details = GenericTripDetails(
            stations = listOf("Amsterdam", "Utrecht", "Eindhoven"),
            times = listOf(
                Pair(t0, t1), // Amsterdam to Utrecht
                Pair(t1, t2)  // Utrecht to Eindhoven
            )
        )

        // Trip 2: "Rotterdam" -> "Utrecht" -> "Arnhem"
        val trip2Details = GenericTripDetails(
            stations = listOf("Rotterdam", "Utrecht", "Arnhem"),
            times = listOf(
                Pair(t0, t1), // Rotterdam to Utrecht
                Pair(t2, t3)  // Utrecht to Arnhem
            )
        )

        val input = mapOf(
            "T1" to trip1Details,
            "T2" to trip2Details
        )

        val result = toInternalRepresentation(input)

        // Verify keys mapping
        assertEquals(listOf("T1", "T2"), result.tripKey)
        // Order of discovered stations should preserve insertion order
        // Amsterdam, Utrecht, Eindhoven, Rotterdam, Arnhem
        assertEquals(listOf("Amsterdam", "Utrecht", "Eindhoven", "Rotterdam", "Arnhem"), result.stationKey)

        // Verify Trip instances
        val internalTrips = result.internalTrips
        assertEquals(2, internalTrips.size)

        // Amsterdam is station index 0, Utrecht is 1, Eindhoven is 2
        val t1Internal = internalTrips[0]
        assertEquals(listOf(0, 1, 2), t1Internal.stations)
        assertEquals(trip1Details.times, t1Internal.legs)

        // Rotterdam is station index 3, Utrecht is 1, Arnhem is 4
        val t2Internal = internalTrips[1]
        assertEquals(listOf(3, 1, 4), t2Internal.stations)
        assertEquals(trip2Details.times, t2Internal.legs)

        // Verify Station instances
        val internalStations = result.internalStations
        assertEquals(5, internalStations.size)

        // Station 0 (Amsterdam): visited by Trip 0 ("T1")
        assertEquals(listOf(0), internalStations[0].trips)
        // Station 1 (Utrecht): visited by Trip 0 ("T1") and Trip 1 ("T2")
        assertEquals(listOf(0, 1), internalStations[1].trips)
        // Station 2 (Eindhoven): visited by Trip 0 ("T1")
        assertEquals(listOf(0), internalStations[2].trips)
        // Station 3 (Rotterdam): visited by Trip 1 ("T2")
        assertEquals(listOf(1), internalStations[3].trips)
        // Station 4 (Arnhem): visited by Trip 1 ("T2")
        assertEquals(listOf(1), internalStations[4].trips)
    }

    @Test
    fun testTripTimeAccessors() {
        val t0 = Instant.fromEpochMilliseconds(1000)
        val t1 = Instant.fromEpochMilliseconds(2000)
        val t2 = Instant.fromEpochMilliseconds(3000)

        // Trip visits station 10, then 11, then 12
        val trip = Trip(
            stations = listOf(10, 11, 12),
            legs = listOf(
                Pair(t0, t1),
                Pair(t1, t2)
            )
        )

        // Test departTimeAtStation
        assertEquals(t0, trip.departTimeAtStation(10))
        assertEquals(t1, trip.departTimeAtStation(11))
        assertNull(trip.departTimeAtStation(12)) // Terminal station doesn't depart
        assertNull(trip.departTimeAtStation(99)) // Non-visiting station

        // Test arrivalTimeAtStation
        assertNull(trip.arrivalTimeAtStation(10)) // Origin station doesn't arrive
        assertEquals(t1, trip.arrivalTimeAtStation(11))
        assertEquals(t2, trip.arrivalTimeAtStation(12))
        assertNull(trip.arrivalTimeAtStation(99)) // Non-visiting station

        // Test departTimeAtStationIndex
        assertEquals(t0, trip.departTimeAtStationIndex(0))
        assertEquals(t1, trip.departTimeAtStationIndex(1))

        // Test arrivalTimeAtStationIndex
        assertEquals(t1, trip.arrivalTimeAtStationIndex(1))
        assertEquals(t2, trip.arrivalTimeAtStationIndex(2))
    }
}
