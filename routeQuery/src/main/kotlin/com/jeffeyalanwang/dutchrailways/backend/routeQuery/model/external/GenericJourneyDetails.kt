package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external

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