package com.jeffeyalanwang.dutchrailways.backend.server.processing

import kotlin.time.Instant

/**
 * @property stations A list of station identifiers.
 * @property times    A list of pairs:
 *  * The departure time of the trip at `stations[i]`
 *  * The arrival time of the trip at `stations[i + 1]`
 */
class GenericTripDetails<EStation>(
    val stations: List<EStation>,
    val times: List<Pair<Instant, Instant>>,
)

/**
 * @property legs           A list of pairs:
 *  * The station identifier at the departure of the trip
 *  * The trip identifier of the trip taken from the station to the next
 *    station in the list, or [finalStation] if this is the last leg.
 * @property finalStation   The station identifier at the arrival of the trip.
 */
class GenericJourneyDetails<ETrip, EStation>(
    val legs: List<Pair<EStation, ETrip>>,
    val finalStation: EStation,
)

fun interface RouteQueryStrategy<ETrip, EStation> {
    fun invoke(
        origin: ETrip,
        destination: ETrip,
        startTime: Instant,
        stopDetails: Map<ETrip, GenericTripDetails<EStation>>,
    ): List<GenericJourneyDetails<ETrip, EStation>>
}

fun interface RangeRouteQueryStrategy<ETrip, EStation> {
    /**
     * @param endTime   Inclusive.
     */
    fun invoke(
        origin: ETrip,
        destination: ETrip,
        startTime: Instant,
        endTime: Instant,
        stopDetails: Map<ETrip, GenericTripDetails<EStation>>,
    ): List<GenericJourneyDetails<ETrip, EStation>>
}
