package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey

/**
 * Allows the use of contiguous array pointers in algorithm processing,
 * improving CPU cache locality.
 */
internal interface DataConverter<ETrip, EStation> {
    fun EStation.convertToInternal(): StationId

    fun TripId.convertToExternal(): ETrip
    fun StationId.convertToExternal(): EStation

    fun Journey.Leg.convertToExternal() =
        GenericJourneyDetails.Leg(
            originStation = originStation.convertToExternal(),
            trip = trip.convertToExternal(),
        )

    fun Journey.convertToExternal() =
        GenericJourneyDetails(
            legs = legs.map { it.convertToExternal() },
            finalStation = finalStation.convertToExternal(),
        )

    fun List<Journey>.convertToExternal() =
        map { it.convertToExternal() }
}
