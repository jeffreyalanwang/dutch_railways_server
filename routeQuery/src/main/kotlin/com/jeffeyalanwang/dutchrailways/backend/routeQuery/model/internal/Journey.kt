package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal

internal data class Journey(
    val legs: List<Leg>,
    val finalStation: Int,
) {
    internal data class Leg(
        val originStation: Int,
        val trip: Int,
    )
}