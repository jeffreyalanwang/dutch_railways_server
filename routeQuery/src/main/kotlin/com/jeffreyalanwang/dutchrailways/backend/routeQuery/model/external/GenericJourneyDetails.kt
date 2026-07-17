package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external

/**
 * The public return value.
 *
 * @property legs           A list of pairs:
 *  * The station identifier at the departure of the trip
 *  * The trip identifier of the trip taken from the station to the next
 *    station in the list, or [finalStation] if this is the last leg.
 * @property finalStation   The station identifier at the arrival of the trip.
 */
class GenericJourneyDetails<ETrip, EStation>(
    val legs: List<Leg<ETrip, EStation>>,
    val finalStation: EStation,
) {
    data class Leg<ETrip, EStation>(
        val originStation: EStation,
        val trip: ETrip,
    )
}