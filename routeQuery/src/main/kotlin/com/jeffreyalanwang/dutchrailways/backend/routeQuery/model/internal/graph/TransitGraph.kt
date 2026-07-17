package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip

internal interface TransitGraph {
    operator fun get(tripId: TripId): Trip
    operator fun get(stationId: StationId): Station

    val tripCount: Int
    val stationCount: Int

    fun TripId.withData() = this to get(this)
    fun List<TripId>.withData() = map { it.withData() }
}

internal inline fun <reified K: GraphId> TransitGraph.countOf() =
    when (K::class) {
        TripId::class -> tripCount
        StationId::class -> stationCount
        else -> error("Unsupported type: ${K::class}")
    }