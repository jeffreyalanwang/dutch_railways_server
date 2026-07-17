package com.jeffreyalanwang.dutchrailways.backend.server.app.api

import com.jeffreyalanwang.dutchrailways.backend.dataSource.Stop
import com.jeffreyalanwang.dutchrailways.backend.server.app.processing.JourneyFinder
import org.springframework.context.annotation.ComponentScan
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import kotlin.time.Instant

@ComponentScan("com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl")
@Controller
class ApiController(
    private val journeyFinder: JourneyFinder,
) {
    @QueryMapping
    fun findJourneys(
        @Argument originStation: Int,
        @Argument destinationStation: Int,
        @Argument earliestDepartTime: Instant,
        @Argument latestArriveTime: Instant? = null,
        @Argument maxCount: Int? = null,
    ): List<Stop> =
        journeyFinder(
            originStation = originStation,
            destinationStation = destinationStation,
            earliestDepartTime = earliestDepartTime,
            latestArriveTime = latestArriveTime,
        ).run {
            if (maxCount == null) this
            else take(maxCount)
        }
}