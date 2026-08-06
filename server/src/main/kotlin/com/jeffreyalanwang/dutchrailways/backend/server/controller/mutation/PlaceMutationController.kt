package com.jeffreyalanwang.dutchrailways.backend.server.controller.mutation

import com.jeffreyalanwang.dutchrailways.api.MutationArea
import com.jeffreyalanwang.dutchrailways.api.MutationStation
import com.jeffreyalanwang.dutchrailways.backend.server.repository.AreaRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Controller
class PlaceMutationController(
    val areaRepository: AreaRepository,
    val stationRepository: StationRepository,
) {

    @Transactional
    @MutationMapping
    fun updateArea(@Argument id: Int, @Argument details: MutationArea): Area? =
        areaRepository.findById(id)
        .getOrNull()
        ?.run {
            name = details.name

            areaRepository.save(this)
        }

    // TODO modification of location should modify parent area

    @Transactional
    @MutationMapping
    fun updateStation(@Argument id: Int, @Argument details: MutationStation): Station? =
        stationRepository.findById(id)
        .getOrNull()
        ?.run {
            name = details.name
            address = details.address
            geom = details.geom.toGeoLatte()

            stationRepository.save(this)
        }

}
