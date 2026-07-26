package com.jeffreyalanwang.dutchrailways.api

import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import java.time.OffsetDateTime

interface MutationPassService {
    val name: String
    val trainset: Trainset
    val amenities: Collection<Amenity>
    val stops: List<MutationStop>
}

interface MutationStop {
    val station: Int
    val arriveTime: OffsetDateTime?
    val departTime: OffsetDateTime?
}

interface MutationArea {
    val name: String
}

interface MutationStation {
    val name: String
    val address: String
    val geom: GeoCoords
}