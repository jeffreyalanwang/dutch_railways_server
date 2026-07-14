package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal

import kotlin.time.Instant

/**
 * @property stations       Index of station in their master list.
 * @property legs           `legs[i]` stores the time of depature from
 *                          `stations[i]` and the time of arrival to
 *                          stations[i + 1].
 */
internal data class Trip(
    val stations: List<Int>,
    val legs: List<Pair<Instant, Instant>>,
) {
    /**
     * @return  Time that this trip departs [station],
     *          or `null` if:
     *          * It does not visit [station]
     *          * It terminates at [station]
     */
    fun departTimeAtStation(station: Int) =
        stations.indexOf(station)
            .takeUnless { it < 0 || it >= legs.size }
            ?.let { legs[it].first }

    fun arrivalTimeAtStation(station: Int) =
        stations.indexOf(station).minus(1)
            .takeUnless { it < 0 }
            ?.let { legs[it].second }

    fun departTimeAtStationIndex(index: Int) =
        legs[index].first

    fun arrivalTimeAtStationIndex(index: Int) =
        legs[index - 1].second
}