package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.jeffreyalanwang.dutchrailways.backend.server.dto.GeoLatteGeoCoords
import com.jeffreyalanwang.dutchrailways.backend.server.dto.GeoLatteGeoMultiPolygon
import com.jeffreyalanwang.dutchrailways.backend.server.dto.PointJourney
import com.jeffreyalanwang.dutchrailways.backend.server.processing.JourneyFinder
import com.jeffreyalanwang.dutchrailways.backend.server.repository.AreaRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import com.jeffreyalanwang.dutchrailways.backend.server.repository.joinedOn
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller
import java.time.Instant

@Controller
class GraphQlController(
    private val journeyFinder: JourneyFinder,

    private val passServiceRepository: PassServiceRepository,
    private val areaRepository: AreaRepository,
    private val stationRepository: StationRepository,

    batchLoaderRegistry: BatchLoaderRegistry,
) {
    init {
        batchLoaderRegistry.run {
            forTypePair<Int, PassService>() .registerBatchLoader { ids ->
                passServiceRepository   .findAllById(ids).joinedOn(ids) { it.id }.map { it!! }
            }
            forTypePair<Int, Area>()        .registerBatchLoader { ids ->
                areaRepository          .findAllById(ids).joinedOn(ids) { it.id }.map { it!! }
            }
            forTypePair<Int, Station>()     .registerBatchLoader { ids ->
                stationRepository       .findAllById(ids).joinedOn(ids) { it.id }.map { it!! }
            }
        }
    }

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

    @QueryMapping
    context(dataLoader: DataLoader<Int, PassService>)
    fun passServiceById(@Argument("id") id: Int) = dataLoader.load(id)

    @QueryMapping
    context(dataLoader: DataLoader<Int, Area>)
    fun areaById(@Argument("id") id: Int) = dataLoader.load(id)

    @QueryMapping
    context(dataLoader: DataLoader<Int, Station>)
    fun stationById(@Argument("id") id: Int) = dataLoader.load(id)

    @QueryMapping
    fun stopOfPassServiceAtStation(@Argument passService: Int, @Argument station: Int) =
        passServiceRepository.getStop(serviceId = passService, stationId = station)

    // Below: implementations of schema mappings.
    // Only required when:
    //  * property names differ between Java entity/DTO and GraphQL schema type, or
    //  * when cached/bulk/async query ability is desired via a [dataLoader]

    @SchemaMapping
    fun PassService.trainset() = consist?.enum

    @SchemaMapping
    fun PassService.amenities() = consist?.amenities?.map { it.enum }

    @SchemaMapping
    fun PassService.stops(@Argument after: Instant, @Argument maxCount: Int) =
        passServiceRepository.getStops(id, after, maxCount)

    @SchemaMapping
    fun Station.stops(@Argument after: Instant, @Argument maxCount: Int) =
        passServiceRepository.getStops(id, after, maxCount)

    @SchemaMapping
    fun Station.geom() = geom?.let { GeoLatteGeoCoords(it) }

    @SchemaMapping
    fun Area.geom() = geom?.let { GeoLatteGeoMultiPolygon(it) }

    @SchemaMapping(typeName = "Stop")
    context(dataLoader: DataLoader<Int, PassService>)
    fun Stop.passService() = dataLoader.load(serviceId)

    @SchemaMapping(typeName = "Stop")
    context(dataLoader: DataLoader<Int, Station>)
    fun Stop.station() = dataLoader.load(stationId)

    private val Stop.timeZone get() = stationRepository.getTimeZone(stationId)

    @SchemaMapping
    fun Stop.arriveTime() = arriveTime?.run { atZone(timeZone).toOffsetDateTime() }

    @SchemaMapping
    fun Stop.departTime() = departTime?.run { atZone(timeZone).toOffsetDateTime() }

    @SchemaMapping(typeName = "JourneyPoint")
    context(dataLoader: DataLoader<Int, Station>)
    fun PointJourney.Point.place() = dataLoader.load(station)

    @SchemaMapping(typeName = "JourneyPoint")
    context(dataLoader: DataLoader<Int, PassService>)
    fun PointJourney.Point.via() = passService?.let { dataLoader.load(it) }
}
