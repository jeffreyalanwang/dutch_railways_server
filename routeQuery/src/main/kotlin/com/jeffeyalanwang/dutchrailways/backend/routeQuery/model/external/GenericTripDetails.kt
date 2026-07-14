package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external

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