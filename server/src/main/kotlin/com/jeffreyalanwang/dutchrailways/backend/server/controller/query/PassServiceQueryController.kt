package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.PointJourney.JourneyPoint
import com.jeffreyalanwang.dutchrailways.backend.server.controller.forTypePair
import com.jeffreyalanwang.dutchrailways.backend.server.controller.registerBatchLoader
import com.jeffreyalanwang.dutchrailways.api.Trainset
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import com.jeffreyalanwang.dutchrailways.backend.server.repository.joinedOn
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller
import java.time.Instant
import com.jeffreyalanwang.dutchrailways.api.Amenity as AmenityEnum
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Amenity as AmenityEntity

@Controller
class PassServiceQueryController(
    private val passServiceRepository: PassServiceRepository,
    batchLoaderRegistry: BatchLoaderRegistry,
) {
    init {
        batchLoaderRegistry.forTypePair<Int, PassService>()
            .registerBatchLoader { ids ->
                passServiceRepository.findAllById(ids)
                    .joinedOn(ids) { it.id }
                    .map { it!! }
            }
    }

    @QueryMapping
    fun passServiceById(@Argument id: Int, dataLoader: DataLoader<Int, PassService>) = dataLoader.load(id)

    @QueryMapping
    fun stopOfPassServiceAtStation(@Argument passService: Int, @Argument station: Int) =
        passServiceRepository.getStop(serviceId = passService, stationId = station)

    @QueryMapping
    fun defaultAmenitiesOf(@Argument trainset: Trainset): List<AmenityEnum> =
        passServiceRepository
            .getTrainsetEntity(trainset)
            .amenities.toEnums()

    @SchemaMapping
    fun PassService.trainset(): Trainset? = consist?.run { enumValueOf(name) }

    @SchemaMapping
    fun PassService.amenities(): Collection<AmenityEnum>? = consist?.amenities?.toEnums()

    @SchemaMapping
    fun PassService.stops(@Argument after: Instant, @Argument maxCount: Int) =
        passServiceRepository.getStops(id, after, maxCount)

    @SchemaMapping
    fun Stop.passService(dataLoader: DataLoader<Int, PassService>) = dataLoader.load(serviceId)

    @SchemaMapping
    fun JourneyPoint.via(dataLoader: DataLoader<Int, PassService>) = passService?.let { dataLoader.load(it) }
}

private fun Iterable<AmenityEntity>.toEnums() = map { enumValueOf<AmenityEnum>(it.description) }
