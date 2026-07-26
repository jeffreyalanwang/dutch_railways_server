package com.jeffreyalanwang.dutchrailways.backend.server.dto

import com.jeffreyalanwang.dutchrailways.api.MutationArea
import com.jeffreyalanwang.dutchrailways.api.MutationStation
import com.jeffreyalanwang.dutchrailways.api.MutationStop
import com.jeffreyalanwang.dutchrailways.api.Trainset
import org.springframework.data.web.ProjectedPayload
import com.jeffreyalanwang.dutchrailways.api.Amenity as AmenityEnum

@ProjectedPayload
interface MutationPassService {
    val name: String
    val trainset: Trainset
    val stops: List<MutationStop>
    val amenities: Collection<String>

    // Oddly, Spring can implicitly project inputs when they are enums (e.g. [trainset]),
    // as well as when they are lists of objects or interfaces ([stops]), but it seems that
    // it cannot handle lists of enums; so, we use a default property getter.
    val amenityEnums: Collection<AmenityEnum> get() = amenities.map { enumValueOf(it) }
}

@ProjectedPayload
interface MutationStop: MutationStop

@ProjectedPayload
interface MutationArea: MutationArea

@ProjectedPayload
interface MutationStation: MutationStation

