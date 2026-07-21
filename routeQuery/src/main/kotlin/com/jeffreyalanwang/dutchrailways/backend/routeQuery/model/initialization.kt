@file:OptIn(ExperimentalContracts::class)

package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.isSortedAndUnique
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.withIndexOrPut
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private typealias StationLookupRequester<EStation> = (TripId, EStation) -> StationId

/**
 * Build a list of [com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station] objects.
 *
 * This overload performs lookups in a pre-flattened list of stations.
 *
 * @param stationLookupBlock    A procedure that provides trip IDs and
 *                              requires the associated station IDs.
 */
internal fun <EStation, R> buildGraphStations(
    stations: Iterable<EStation>,
    stationLookupBlock: (StationLookupRequester<EStation>) -> R,
): Pair<List<Station>, R> {
    contract {
        callsInPlace(stationLookupBlock, InvocationKind.EXACTLY_ONCE)
    }

    // Allow quick lookup, given a [KStation], of
    // its ID and the running list of visiting trips.
    val stationMap = stations.withIndex().associate { (i, it) ->
        it to Pair(
            i,
            mutableListOf<TripId>(),
        )
    }

    val r = stationLookupBlock { tripId, eStation ->
        val (stationMasterIndex, stationTripIdList) = stationMap[eStation]!!

        // Side effect: add to the station's list.
        stationTripIdList += tripId

        StationId(stationMasterIndex)
    }

    // This operation relies on the fact that maps preserve insertion order.
    return stationMap.values.map { (_, tripIds) -> Station(tripIds) } to r

}

/**
 * Build a list of [Station] objects.
 *
 * This overload generates a flattened list of stations as it goes.
 *
 * @param stationLookupBlock    A procedure that provides trip IDs and
 *                              requires the associated station IDs.
 */
internal fun <EStation, R> buildGraphStations(
    stationLookupBlock: (StationLookupRequester<EStation>) -> R,
): Triple<List<Station>, List<EStation>, R> {
    contract {
        callsInPlace(stationLookupBlock, InvocationKind.EXACTLY_ONCE)
    }

    // LinkedHashMap preserves insertion order.
    val stationMap = LinkedHashMap<EStation, LinkedList<TripId>>()

    val r = stationLookupBlock { tripId, eStation ->
        val (index, tripIdList) = stationMap.withIndexOrPut(eStation) { LinkedList() }

        tripIdList += tripId

        StationId(index)
    }

    val stationsExternal = stationMap.keys.toList()
    val stationsInternal = stationMap.values.map { Station(trips = it) }

    return Triple(stationsInternal, stationsExternal, r)

}

internal fun <EStation> Iterable<GenericTripDetails<EStation>>.generateGraphTrips(
    onStationLookup: (TripId, EStation) -> StationId,
) = mapIndexed { index, details ->
        val tripId = TripId(index)

        details.generateGraphTrip { externalStation ->
            onStationLookup(tripId, externalStation)
        }
    }

private fun <EStation> GenericTripDetails<EStation>.generateGraphTrip(
    onStationLookup: (EStation) -> StationId,
): Trip {
    require(times.isSortedAndUnique())

    val stations =
        stations.map { externalStation -> onStationLookup(externalStation) }

    val (departTimes, arriveTimes) =
        Trip.departArriveTimesFrom(times.map { it.departTime to it.arriveTime })

    return Trip(
        stations = stations,
        departTimes = departTimes,
        arriveTimes = arriveTimes
    )
}