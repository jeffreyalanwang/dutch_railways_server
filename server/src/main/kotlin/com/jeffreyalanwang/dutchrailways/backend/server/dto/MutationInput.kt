package com.jeffreyalanwang.dutchrailways.backend.server.dto

import org.springframework.data.web.ProjectedPayload
import java.time.OffsetDateTime

@ProjectedPayload
interface MutationPassService {
    val name: String
    val trainset: TrainsetTypeEnum
    val amenities: Collection<String>
    val stops: List<MutationStop>

    // Oddly, Spring can implicitly project inputs when they are enums (e.g. [trainset]),
    // as well as when they are lists of objects or interfaces ([stops]), but it seems that
    // it cannot handle lists of enums; so, we use a default property getter.
    val amenityEnums: Collection<AmenityEnum> get() = amenities.map { enumValueOf(it) }
}

@ProjectedPayload
interface MutationStop {
    val station: Int
    val arriveTime: OffsetDateTime?
    val departTime: OffsetDateTime?
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
