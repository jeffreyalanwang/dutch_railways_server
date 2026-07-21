package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class InternalRepresentationTest {

    @Test
    fun testDataConverter() {
        // Prepare some sample stations and times
        val t0 = Instant.fromEpochMilliseconds(1000)
        val t1 = Instant.fromEpochMilliseconds(2000)
        val t2 = Instant.fromEpochMilliseconds(3000)
        val t3 = Instant.fromEpochMilliseconds(4000)

        // Trip 1: "Amsterdam" -> "Utrecht" -> "Eindhoven"
        val trip1Details = GenericTripDetails.of(
            stations = listOf("Amsterdam", "Utrecht", "Eindhoven"),
            times = listOf(
                Pair(t0, t1), // Amsterdam to Utrecht
                Pair(t1, t2)  // Utrecht to Eindhoven
            )
        )

        // Trip 2: "Rotterdam" -> "Utrecht" -> "Arnhem"
        val trip2Details = GenericTripDetails.of(
            stations = listOf("Rotterdam", "Utrecht", "Arnhem"),
            times = listOf(
                Pair(t0, t1), // Rotterdam to Utrecht
                Pair(t2, t3)  // Utrecht to Arnhem
            )
        )

        val result = RouteQueryDataSource.fromTrips(
            "T1" to trip1Details,
            "T2" to trip2Details,
        )

        assertEquals(2, result.transitGraph.tripCount)
        assertEquals(5, result.transitGraph.stationCount)
    }

    @Test
    fun testTripTimeAccessors() {
        val t0 = Instant.fromEpochMilliseconds(1000)
        val t1 = Instant.fromEpochMilliseconds(2000)
        val t2 = Instant.fromEpochMilliseconds(3000)

        // Trip visits station 10, then 11, then 12
        val trip = Trip.fromLiterals(
            stations = listOf(10, 11, 12),
            legs = listOf(
                t0 to t1,
                t1 to t2
            )
        )

        // Test arrive/depart time at station

        assertEquals(t0, trip.departTimeAt(StationId(10)))
        assertEquals(t1, trip.departTimeAt(StationId(11)))
        assertNull(trip.departTimeAt(StationId(12))) // Terminal station doesn't depart
        assertNull(trip.departTimeAt(StationId(99))) // Non-visiting station

        assertNull(trip.arriveTimeAt(StationId(10))) // Origin station doesn't arrive
        assertEquals(t1, trip.arriveTimeAt(StationId(11)))
        assertEquals(t2, trip.arriveTimeAt(StationId(12)))
        assertNull(trip.arriveTimeAt(StationId(99))) // Non-visiting station

        // Test arrive/depart time at station index

        assertEquals(t0, trip.departTimeAt(0))
        assertEquals(t1, trip.departTimeAt(1))

        assertEquals(t1, trip.arriveTimeAt(1))
        assertEquals(t2, trip.arriveTimeAt(2))
    }
}
