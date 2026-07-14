package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal

/**
 * @property trips          Index of trip in their master list.
 */
internal data class Station(
    val trips: List<Int>,
)