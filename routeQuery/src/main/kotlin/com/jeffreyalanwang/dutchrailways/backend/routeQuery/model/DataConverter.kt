package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey

class StationNotFoundException : IllegalArgumentException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(
        externalStation: Any,
        stationsKey: List<Any>,
    ) : this(
        "Station $externalStation not found in graph " +
        "(containing total of ${stationsKey.size} stations)."
    )

    constructor(
        station: Any,
        cause: Throwable,
    ) : this(
        "Station $station not found in graph.",
        cause,
    )
}

/**
 * Allows the use of contiguous array pointers in algorithm processing,
 * improving CPU cache locality.
 */
internal interface DataConverter<ETrip, EStation> {

    @Throws(StationNotFoundException::class)
    fun EStation.convertToInternal(): StationId

    fun TripId.convertToExternal(): ETrip
    fun StationId.convertToExternal(): EStation

    fun Journey.Leg.convertToExternal() =
        GenericJourneyDetails.LegStartPoint(
            originStation = originStation.convertToExternal(),
            trip = trip.convertToExternal(),
        )

    fun Journey.convertToExternal() =
        GenericJourneyDetails(
            legStartPoints = legs.map { it.convertToExternal() },
            finalStation = finalStation.convertToExternal(),
        )

    fun List<Journey>.convertToExternal() =
        map { it.convertToExternal() }
}
