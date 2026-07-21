package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Table
@Entity
class TrainsetType (
    @Column(length = 64)
    @Id var name: String,

    @ManyToMany
    @JoinTable(name = "trainsetamenities")
    var amenities: MutableSet<Amenity>,
)
