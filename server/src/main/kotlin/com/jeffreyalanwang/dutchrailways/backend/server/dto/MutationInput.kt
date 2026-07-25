package com.jeffreyalanwang.dutchrailways.backend.server.dto

import org.springframework.data.web.ProjectedPayload
import java.time.Instant

@ProjectedPayload
interface MutationPassService {
    val name: String
    val trainset: TrainsetTypeEnum
    val amenities: List<AmenityEnum>
    val stops: List<MutationStop>
}

@ProjectedPayload
interface MutationStop {
    val station: Int
    val arriveTime: Instant?
    val departTime: Instant?
}

@ProjectedPayload
interface MutationArea {
    val name: String
}

@ProjectedPayload
interface MutationStation {
    val name: String
    val address: String
    val geom: GeoCoords
}
