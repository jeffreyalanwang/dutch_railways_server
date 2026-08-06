package com.jeffreyalanwang.dutchrailways.backend.server.repository.dto

import java.time.Instant
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop as StopEntity

// Data from repository for consumption by
// [com.jeffreyalanwang.dutchrailways.backend.server.processing.journey.JourneyFinder].

data class PassServiceTimetable(
    val id: Int = 0,
    val stops: List<Stop>,
) {

    interface Stop {
        val arriveTime: Instant?
        val departTime: Instant?
        val stationId: Int
    }

    companion object {

        fun fromStopEntities(
            stops: List<StopEntity>,
            id: Int = stops.first().serviceId,
        ) = PassServiceTimetable(
            id = id,
            stops = stops.map {
                StopFromEntity(it)
            },
        )

    }

}

private class StopFromEntity(val entity: StopEntity) : PassServiceTimetable.Stop {
    override val arriveTime get() = entity.arriveTime
    override val departTime get() = entity.departTime
    override val stationId get() = entity.stationId
}