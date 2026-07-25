package com.jeffreyalanwang.dutchrailways.backend.server.dto

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
        fun Collection<Amenity>.toEnums() = map { valueOf(it.description) }

        private fun <T> Collection<AmenityEnum>.isLike(other: Collection<T>, nameSelector: (T) -> String): Boolean {
            if (this.size != other.size) return false
            val set = this.mapTo(HashSet(size)) { it.name }
            return other.all { nameSelector(it) in set }
        }

        @JvmName("CollectionEnumIsLikeNames")
        infix fun Collection<AmenityEnum>.isLike(names: Collection<String>) =  isLike(names) { it }

        @JvmName("CollectionEnumIsLikeEntities")
        infix fun Collection<AmenityEnum>.isLike(entities: Collection<Amenity>) = isLike(entities) { it.description }

        @JvmName("CollectionEntitiesIsLikeEnums")
        infix fun Collection<Amenity>.isLike(enums: Collection<AmenityEnum>) = enums isLike this
    }
}