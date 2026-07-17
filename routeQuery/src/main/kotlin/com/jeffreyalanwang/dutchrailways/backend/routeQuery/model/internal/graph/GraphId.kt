package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph

internal sealed interface GraphId {
    val index: Int
}

/**
 * A strongly-typed index in the master list for [com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip].
 */
@JvmInline
internal value class TripId(override val index: Int): GraphId

/**
 * A strongly-typed index in the master list for [com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station].
 */
@JvmInline
internal value class StationId(override val index: Int): GraphId
