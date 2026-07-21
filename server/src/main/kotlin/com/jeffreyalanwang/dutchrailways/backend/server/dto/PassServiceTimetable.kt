package com.jeffreyalanwang.dutchrailways.backend.server.dto

import java.io.Serializable
import java.time.OffsetDateTime
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop as StopEntity

/**
 * Data for consumption by [com.jeffreyalanwang.dutchrailways.backend.server.api.GraphQlController.journeyFinder].
 */
data class PassServiceTimetable(
    val id: Int = 0,
    val stops: List<Stop>,
) : Serializable {

    interface Stop : Serializable {
        val arriveTime: OffsetDateTime?
        val departTime: OffsetDateTime?
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