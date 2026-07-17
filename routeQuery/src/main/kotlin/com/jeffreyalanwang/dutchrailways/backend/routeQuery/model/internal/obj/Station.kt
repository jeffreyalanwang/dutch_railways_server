package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId

/**
 * @property trips          Index of trip in their master list.
 */
internal data class Station(
    val trips: List<TripId>,
)
