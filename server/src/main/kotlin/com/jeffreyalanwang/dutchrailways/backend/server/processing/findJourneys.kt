package com.jeffreyalanwang.dutchrailways.backend.server.processing

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RangeRouteQueryStrategy
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RouteQueryDataSource
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RouteQueryStrategy
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.RRaptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.Raptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.StationNotFoundException
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails.Leg
import com.jeffreyalanwang.dutchrailways.backend.server.dto.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StopRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

@Component
class JourneyFinder(
    private val stationRepository: StationRepository,
    private val stopRepository: StopRepository,

    private val routeStrategy: RouteQueryStrategy,
    private val rangeRouteStrategy: RangeRouteQueryStrategy,
) {
    /**
     * @return A list of journeys, or null if origin and destination are the same.
     * @throws StationNotFoundException
     * // TODO handle StationNotFoundException in api
     */
    operator fun invoke(
        originStation: Int,
        destinationStation: Int,
        earliestDepartTime: Instant,
        latestArriveTime: Instant? = null,
    ): List<PointJourney> {
        if (originStation == destinationStation) {
            return listOf(
                hydrateJourney(originAndDestination = originStation, startTime = earliestDepartTime),
            )
        }

        val journeys = with(graph) {
            if (latestArriveTime == null) {
                routeStrategy(
                    origin = originStation,
                    destination = destinationStation,
                    startTime = earliestDepartTime,
                )
            } else {
                rangeRouteStrategy(
                    origin = originStation,
                    destination = destinationStation,
                    timeRange = earliestDepartTime..latestArriveTime,
                )
            }
        }

        return journeys.map { hydrateJourney(it) }
    }

    private val graph: RouteQueryDataSource<Int, Int>
        get() {
            val trips =
                stopRepository.getAllPassServiceTimetables().map { trip ->
                    trip.id to GenericTripDetails(
                        stations = trip.stops.map { it.stationId },
                        times = trip.stops
                            .zipWithNext { a, b ->
                                Leg(
                                    a.departTime!!.toInstant().toKotlinInstant(),
                                    b.arriveTime!!.toInstant().toKotlinInstant(),
                                )
                            },
                    )
                }
            val stations = stationRepository.getAllStationIds()

            return RouteQueryDataSource.fromRelational(
                trips = trips,
                stations = stations,
            )
        }

    /**
     * This overload is responsible for zero-leg journeys.
     */
    private fun hydrateJourney(originAndDestination: Int, startTime: Instant) =
        try {
            with(stationRepository) {
                PointJourney.ofSingleStop(
                    startTime.atOffsetOf(originAndDestination) to originAndDestination
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            throw StationNotFoundException(originAndDestination, e)
        }

    /**
     * Calls [GenericJourneyDetails.toFlatStops] and expects a
     * non-null result, so requires that journeys have at least one
     * leg (i.e., start station differs from end station).
     *
     * This implementation performs a bulk query.
     */
    private fun hydrateJourney(journey: GenericJourneyDetails<Int, Int>): PointJourney =
        journey.toFlatStops { trip, station, _ -> trip to station }!!
            .let { stopRepository.getStopsByServiceAndStation(keyList = it) }
            .mapIndexed { index, stop ->
                PointJourney.Point(
                    stationId = stop.stationId,
                    time = if (index % 2 == 0) stop.departTime!! else stop.arriveTime!!,
                    passService = stop.serviceId
                )
            }
            .let { PointJourney(it) }
}

@Configuration
class RouteStrategiesConfiguration {
    @Bean
    fun basicRouteStrategy(): RouteQueryStrategy = Raptor

    @Bean
    fun rangeRouteStrategy(): RangeRouteQueryStrategy = RRaptor
}
