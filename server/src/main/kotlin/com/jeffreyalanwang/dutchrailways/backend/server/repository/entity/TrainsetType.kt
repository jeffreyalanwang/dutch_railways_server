package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Table
@Entity
class TrainsetType (
    @Column(length = 64)
    @Id val name: String,

    @ManyToMany
    @JoinTable(
        "trainsetamenities",
        joinColumns = [JoinColumn("trainsettype")],
        inverseJoinColumns = [JoinColumn("amenity")],
    )
    var amenities: MutableSet<Amenity>,
)