package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.util.GeoMultiPolygon
import com.jeffreyalanwang.dutchrailways.backend.server.controller.forTypePair
import com.jeffreyalanwang.dutchrailways.backend.server.controller.registerBatchLoader
import com.jeffreyalanwang.dutchrailways.backend.server.repository.AreaRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.joinedOn
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller

@Controller
class AreaQueryController(
    private val areaRepository: AreaRepository,
    batchLoaderRegistry: BatchLoaderRegistry,
) {
    init {
        batchLoaderRegistry.forTypePair<Int, Area>()
            .registerBatchLoader { ids ->
                areaRepository.findAllById(ids)
                    .joinedOn(ids) { it.id }
                    .map { it!! }
            }
    }

    @QueryMapping
    fun areaById(@Argument id: Int, dataLoader: DataLoader<Int, Area>) = dataLoader.load(id)

    @SchemaMapping
    fun Area.geom() = geom?.let { GeoMultiPolygon(it) }
}
