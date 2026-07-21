package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.indexOfOrNull
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import kotlin.time.Instant

/**
 * @property departTimes `departTimes[i]` stores the time of depature from `stations[i]`.
 * @property arriveTimes `arriveTimes[i]` stores the time of arrival to `stations[i + 1]`.
 */
internal data class Trip(
    val stations: List<StationId>,
    private val departTimes: List<Instant>,
    private val arriveTimes: List<Instant>,
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
            ?.let { departTimes[it] }

    fun arriveTimeAt(stationId: StationId) =
        stations
            .drop(1)
            .indexOfOrNull(stationId)
            ?.let { arriveTimes[it] }

    fun departTimeAt(tripIndex: Int) =
        departTimes[tripIndex]

    fun arriveTimeAt(tripIndex: Int) =
        arriveTimes[tripIndex - 1]

    companion object {
        /** For testing only. */
        fun fromLiterals(stations: List<Int>, legs: List<Pair<Instant, Instant>>): Trip {
            val (departTimes, arriveTimes) = departArriveTimesFrom(legs)
            return Trip(
                stations = stations.map { StationId(it) },
                departTimes = departTimes,
                arriveTimes = arriveTimes,
            )
        }

        fun departArriveTimesFrom(pairs: List<Pair<Instant, Instant>>) = pairs.unzip()
    }
}
