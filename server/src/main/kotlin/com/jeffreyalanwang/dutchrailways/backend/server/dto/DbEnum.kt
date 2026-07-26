package com.jeffreyalanwang.dutchrailways.backend.server.dto

import com.jeffreyalanwang.dutchrailways.backend.server.repository.SetCompareBuilderScope.Companion.allSetEqual
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Amenity

enum class TrainsetTypeEnum {
    SLT,
    ICM,
    DDZ,
    VIRM,
    SNG,
    ICNG,
    GTW,
    Flirt,
}

enum class AmenityEnum {
    STROOM,
    TOILET,
    WIFI,
    STILTE,
    FIETS,
    TOEGANKELIJK,
    ;
    companion object {
        private val Amenity.name get() = description

        fun Collection<Amenity>.toEnums() = map { valueOf(it.name) }

        @JvmName("CollectionEnumIsLikeNames")
        infix fun Collection<AmenityEnum>.isLike(names: Collection<String>) = allSetEqual {
            thisOn { it.name }
            names on itself
        }

        @JvmName("CollectionEnumIsLikeEntities")
        infix fun Collection<AmenityEnum>.isLike(entities: Collection<Amenity>) = allSetEqual {
            thisOn { it.name }
            entities on { it.name }
        }

        @JvmName("CollectionEntitiesIsLikeEnums")
        infix fun Collection<Amenity>.isLike(enums: Collection<AmenityEnum>) = enums isLike this
    }
}