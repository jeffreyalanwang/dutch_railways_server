package com.jeffeyalanwang.dutchrailways.backend.routeQuery

import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import kotlin.time.Instant

fun interface RangeRouteQueryStrategy<ETrip, EStation> {
    /**
     * @param endTime   Inclusive.
     */
    fun invoke(
        origin: ETrip,
        destination: ETrip,
        timeRange: ClosedRange<Instant>,
        stopDetails: Map<ETrip, GenericTripDetails<EStation>>,
    ): List<GenericJourneyDetails<ETrip, EStation>>
}