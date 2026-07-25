package com.jeffreyalanwang.dutchrailways.backend.server.dto

import java.io.Serializable
import java.time.Instant
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop as StopEntity

/**
 * Data for consumption by [com.jeffreyalanwang.dutchrailways.backend.server.api.AreaQueryController.journeyFinder].
 */
data class PassServiceTimetable(
    val id: Int = 0,
    val stops: List<Stop>,
) : Serializable {

    interface Stop : Serializable {
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