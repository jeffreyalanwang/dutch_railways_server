package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.PointJourney.JourneyPoint
import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import com.jeffreyalanwang.dutchrailways.backend.server.controller.forTypePair
import com.jeffreyalanwang.dutchrailways.backend.server.controller.registerBatchLoader
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
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
import java.time.ZoneId

@Controller
class StationQueryController(
    private val stationRepository: StationRepository,
    batchLoaderRegistry: BatchLoaderRegistry,
) {
    init {
        batchLoaderRegistry.forTypePair<Int, Station>()
            .registerBatchLoader { ids ->
                stationRepository.findAllById(ids)
                    .joinedOn(ids) { it.id }
                    .map { it!! }
            }
    }

    @QueryMapping
    fun stationById(@Argument id: Int, dataLoader: DataLoader<Int, Station>) = dataLoader.load(id)

    @QueryMapping
    fun timeZoneOf(@Argument stationId: Int): ZoneId = stationRepository.getTimeZone(stationId)

    @SchemaMapping
    fun Station.stops(@Argument after: Instant, @Argument maxCount: Int) =
        stationRepository.getStops(id, after, maxCount)

    @SchemaMapping
    fun Station.geom() = geom?.let { GeoCoords(it) }

    @SchemaMapping
    fun Stop.station(dataLoader: DataLoader<Int, Station>) = dataLoader.load(stationId)

    @SchemaMapping
    fun Stop.arriveTime() = arriveTime?.run { atZone(timeZoneOf(stationId)).toOffsetDateTime() }

    @SchemaMapping
    fun Stop.departTime() = departTime?.run { atZone(timeZoneOf(stationId)).toOffsetDateTime() }

    @SchemaMapping
    fun JourneyPoint.place(dataLoader: DataLoader<Int, Station>) = dataLoader.load(station)
}