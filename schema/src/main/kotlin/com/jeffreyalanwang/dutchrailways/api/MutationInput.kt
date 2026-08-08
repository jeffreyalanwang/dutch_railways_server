package com.jeffreyalanwang.dutchrailways.api

import java.time.OffsetDateTime

public data class MutationPassService(
    val name: String,
    val trainset: Trainset,
    val amenities: Collection<Amenity>,
    val stops: List<MutationStop>,
)

public data class MutationStop(
    val station: Int,
    val arriveTime: OffsetDateTime?,
    val departTime: OffsetDateTime?,
)

public data class MutationArea(
    val name: String,
)

public data class MutationStation(
    val name: String,
    val address: String,
    val geom: GeoCoords,
)