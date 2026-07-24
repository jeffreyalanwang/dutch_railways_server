package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Table
@Entity
class TrainsetType (
    @Column(length = 64)
    @Id private val name: String,

    @ManyToMany
    @JoinTable(
        "trainsetamenities",
        joinColumns = [JoinColumn("trainsettype")],
        inverseJoinColumns = [JoinColumn("amenity")],
    )
    var amenities: MutableSet<Amenity>,
) {
    val enum get() = TrainsetTypeEnum.entries.find { it.name == name }
}

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
