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
public class GenericTripDetails<EStation>(
    public val stations: List<EStation>,
    public val times: List<Leg>,
) {
    public class Leg(
        public val departTime: Instant,
        public val arriveTime: Instant,
    )

    public companion object {
        /** For testing only. */
        public fun <EStation> of(
            stations: List<EStation>,
            times: List<Pair<Instant, Instant>>,
        ): GenericTripDetails<EStation> = GenericTripDetails(
            stations,
            times.map { Leg(it.first, it.second) }
        )
    }
}

public fun List<GenericTripDetails.Leg>.isSortedAndUnique(): Boolean =
    isSortedBy { it.departTime } && distinctBy { it.departTime }.size == size