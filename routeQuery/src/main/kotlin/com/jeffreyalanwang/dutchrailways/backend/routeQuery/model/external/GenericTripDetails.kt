package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external

import kotlin.time.Instant

/**
 * The public input value type.
 *
 * @property stations A list of station identifiers.
 * @property times    A list of pairs:
 *  * The departure time of the trip at `stations[i]`
 *  * The arrival time of the trip at `stations[i + 1]`
 */
class GenericTripDetails<EStation>(
    val stations: List<EStation>,
    val times: List<Leg>,
) {
    class Leg(
        val departTime: Instant,
        val arrivalTime: Instant,
    )

    companion object {
        /** For testing only. */
        fun <EStation> of(
            stations: List<EStation>,
            times: List<Pair<Instant, Instant>>,
        ) = GenericTripDetails(
            stations,
            times.map { Leg(it.first, it.second) }
        )
    }
}