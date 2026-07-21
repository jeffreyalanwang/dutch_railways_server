package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.jeffreyalanwang.dutchrailways.backend.server.dto.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.processing.JourneyFinder
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import kotlin.time.Instant

@Controller
class GraphQlController(
    private val journeyFinder: JourneyFinder,
) {
    @QueryMapping
    fun findJourneys(
        @Argument originStation: Int,
        @Argument destinationStation: Int,
        @Argument earliestDepartTime: Instant,
        @Argument latestArriveTime: Instant? = null,
        @Argument maxCount: Int? = null,
    ): List<PointJourney> =
        journeyFinder(
            originStation = originStation,
            destinationStation = destinationStation,
            earliestDepartTime = earliestDepartTime,
            latestArriveTime = latestArriveTime,
        ).run {
            take(maxCount ?: size)
        }
}
