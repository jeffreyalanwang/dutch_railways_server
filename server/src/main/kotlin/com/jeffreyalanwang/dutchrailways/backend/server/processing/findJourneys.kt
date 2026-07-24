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
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.StopType
import com.jeffreyalanwang.dutchrailways.backend.server.dto.PassServiceTimetable
import com.jeffreyalanwang.dutchrailways.backend.server.dto.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import kotlin.time.toKotlinInstant

@Component
class JourneyFinder(
    private val stationRepository: StationRepository,
    private val passServiceRepository: PassServiceRepository,

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
                    startTime = earliestDepartTime.toKotlinInstant(),
                )
            } else {
                rangeRouteStrategy(
                    origin = originStation,
                    destination = destinationStation,
                    timeRange = (earliestDepartTime..latestArriveTime).toKotlinInstantRange(),
                )
            }
        }

        return journeys.map { hydrateJourney(it) }
    }

    private val graph get() = RouteQueryDataSource
        .fromRelational(
            trips = passServiceRepository.getAllTimetables().map { it.asRouteQueryDetails() },
            stations = stationRepository.getAllStationIds(),
        )

    /**
     * This overload is responsible for zero-leg journeys.
     */
    private fun hydrateJourney(originAndDestination: Int, startTime: Instant) =
        try {
            PointJourney.ofSingleStop(
                time = with(stationRepository) { startTime.atOffsetIn(originAndDestination) },
                place = originAndDestination,
            )
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
        journey.toFlatStops { trip, station, stopType -> (trip to station) to stopType }!!
            .let {
                // Perform bulk repository lookup instead of individual mapping
                val (tripToStation, stopType) = it.unzip()
                val stops = passServiceRepository.getStop(serviceIdAndStationId = tripToStation)
                val timeZones = stationRepository.getTimeZone(tripToStation.unzip().second)
                it.indices.map { i -> Triple(stops[i], stopType[i], timeZones[i]) }
            }
            .map { (stop, stopType, timeZone) ->
                stop.asJourneyPoint(
                    isDeparture = when (stopType) {
                        StopType.LEG_START -> true
                        StopType.LEG_END -> false
                    },
                    timeZone = timeZone,
                )
            }
            .let { PointJourney(it) }
}

@Configuration
class RouteStrategiesConfiguration {
    @Bean fun basicRouteStrategy(): RouteQueryStrategy = Raptor
    @Bean fun rangeRouteStrategy(): RangeRouteQueryStrategy = RRaptor
}

// Conversion between Kotlin and Java Instants occurs below.

private fun PassServiceTimetable.asRouteQueryDetails() =
    id to GenericTripDetails(
        stations = stops.map { it.stationId },
        times = stops.zipWithNext { a, b ->
                Leg(departTime = a.departTime!!.toKotlinInstant(), arriveTime = b.arriveTime!!.toKotlinInstant())
            }
    )

private fun Stop.asJourneyPoint(isDeparture: Boolean, timeZone: ZoneId) = PointJourney.Point(
    station = stationId,
    time = (if (isDeparture) departTime!! else arriveTime!!)
        .atZone(timeZone).toOffsetDateTime(),
    passService = serviceId
)

private fun ClosedRange<Instant>.toKotlinInstantRange() =
    start.toKotlinInstant()..endInclusive.toKotlinInstant()