package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId

internal data class Journey(
    val legs: List<Leg>,
    val finalStation: StationId,
) {
    internal data class Leg(
        val originStation: StationId,
        val trip: TripId,
    )
}
