package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.indexOfOrNull
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import kotlin.time.Instant

/**
 * @property legs           `legs[i]` stores the time of depature from
 *                          `stations[i]` and the time of arrival to
 *                          stations[i + 1].
 */
internal data class Trip(
    val stations: List<StationId>,
    val legs: List<Leg>,
) {
    /**
     * @return  Time that this trip departs [stationId],
     *          or `null` if:
     *          * It does not visit [stationId]
     *          * It terminates at [stationId]
     */
    fun departTimeAt(stationId: StationId) =
        stations
            .dropLast(1)
            .indexOfOrNull(stationId)
            ?.let { legs[it].departTime }

    fun arrivalTimeAt(stationId: StationId) =
        stations
            .drop(1)
            .indexOfOrNull(stationId)
            ?.let { legs[it].arrivalTime }

    fun departTimeAt(tripIndex: Int) =
        legs[tripIndex].departTime

    fun arrivalTimeAt(tripIndex: Int) =
        legs[tripIndex - 1].arrivalTime

    data class Leg(
        val departTime: Instant,
        val arrivalTime: Instant,
    )

    companion object {
        /** For testing only. */
        fun fromLiterals(stations: List<Int>, legs: List<Pair<Instant, Instant>>) = Trip(
            stations.map { StationId(it) },
            legs.map { Leg(it.first, it.second) },
        )
    }
}
