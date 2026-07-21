package com.jeffreyalanwang.dutchrailways.backend.server.dto

import java.time.OffsetDateTime

/**
 * A journey represented as a list of time/place points.
 *
 * Corresponds to GraphQL schema.
 */
data class PointJourney(val points: List<Point>) {

    data class Point(
        val time: OffsetDateTime,
        val stationId: Int,
        val passService: Int?,
    )

    companion object {
        fun ofSingleStop(
            timeToPlace: Pair<OffsetDateTime, Int>
        ) = PointJourney(
                points = listOf(
                    Point(
                        time = timeToPlace.first,
                        stationId = timeToPlace.second,
                        passService = null,
                    ),
                )
            )
    }

}
