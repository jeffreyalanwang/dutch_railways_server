package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external

/**
 * The public return value.
 *
 * @property legStartPoints           A list of pairs:
 *  * The station identifier at the departure of the trip
 *  * The trip identifier of the trip taken from the station to the next
 *    station in the list, or [finalStation] if this is the last leg.
 * @property finalStation   The station identifier at the arrival of the trip.
 */
class GenericJourneyDetails<ETrip, EStation>(
    val legStartPoints: List<LegStartPoint<ETrip, EStation>>,
    val finalStation: EStation,
) {
    data class LegStartPoint<ETrip, EStation>(
        val originStation: EStation,
        val trip: ETrip,
    )

    /**
     * Build a list with one [EStop] for each leg's start and end.
     *
     * @return  Null if [legStartPoints] is empty (i.e., start station is [finalStation]).
     *          List alternates between start and end stops.
     */
    fun <EStop> toFlatStops(
        selector: (trip: ETrip, station: EStation, stopType: StopType) -> EStop
    ) = if (legStartPoints.isEmpty()) null
        else buildList<EStop>(legStartPoints.size * 2) {
            for (i in legStartPoints.indices) {
                val (fromStation, trip) = legStartPoints[i]
                val toStation = legStartPoints.getOrNull(i + 1)
                    ?.originStation
                    ?: finalStation

                add(selector(trip, fromStation, StopType.START))
                add(selector(trip, toStation, StopType.END))
            }
        }
}

enum class StopType { START, END }
