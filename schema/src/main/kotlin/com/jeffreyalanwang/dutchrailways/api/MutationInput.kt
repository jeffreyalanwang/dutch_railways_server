package com.jeffreyalanwang.dutchrailways.api

import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import java.time.OffsetDateTime

data class MutationPassService(
    val name: String,
    val trainset: Trainset,
    val amenities: Collection<Amenity>,
    val stops: List<MutationStop>,
)

data class MutationStop(
    val station: Int,
    val arriveTime: OffsetDateTime?,
    val departTime: OffsetDateTime?,
)

data class MutationArea(
    val name: String,
)

data class MutationStation(
    val name: String,
    val address: String,
    val geom: GeoCoords,
)