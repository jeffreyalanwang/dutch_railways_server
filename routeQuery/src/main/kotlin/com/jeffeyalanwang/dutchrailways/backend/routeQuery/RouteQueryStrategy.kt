package com.jeffeyalanwang.dutchrailways.backend.routeQuery

import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import kotlin.time.Instant

fun interface RouteQueryStrategy<ETrip, EStation> {
    fun invoke(
        origin: ETrip,
        destination: ETrip,
        startTime: Instant,
        stopDetails: Map<ETrip, GenericTripDetails<EStation>>,
    ): List<GenericJourneyDetails<ETrip, EStation>>
}