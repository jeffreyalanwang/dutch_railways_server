package com.jeffreyalanwang.dutchrailways.backend.server.app.processing

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RangeRouteQueryStrategy
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RouteQueryStrategy
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.RRaptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.Raptor
import com.jeffreyalanwang.dutchrailways.backend.server.app.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.app.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.dataSource.PassService
import com.jeffreyalanwang.dutchrailways.backend.dataSource.Station
import com.jeffreyalanwang.dutchrailways.backend.dataSource.Stop
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import kotlin.time.Instant

@Component
class JourneyFinder(
    private val passServiceRepository: PassServiceRepository,
    private val stationRepository: StationRepository,

    private val routeStrategy: RouteQueryStrategy<PassService, Station>,
    private val rangeRouteStrategy: RangeRouteQueryStrategy<PassService, Station>,
) {
    operator fun invoke(
        originStation: Int,
        destinationStation: Int,
        earliestDepartTime: Instant,
        latestArriveTime: Instant? = null,
    ): List<Stop> {
        // IDs in the database.
        val tripIds = passServiceRepository.find
        val stationIds = stationRepository.findAll()
        val originStation = stationRepository.findById(originStation)
        val destinationStation = stationRepository.findById(destinationStation)

        if (latestArriveTime != null) {
            rangeRouteStrategy(
                origin = originStation,
                destination = destinationStation,
                startTime = earliestDepartTime,
                stopDetails =
            )
        } else {
            routeStrategy(
                origin = originStation
            )
        }
    }
}

@Configuration
class RouteStrategiesConfiguration {
    @Bean
    fun <TTrip, TStation> routeStrategy(): RouteQueryStrategy<TTrip, TStation> =
        Raptor()

    @Bean
    fun <TTrip, TStation> rangeRouteStrategy(): RangeRouteQueryStrategy<TTrip, TStation> =
        RRaptor()
}