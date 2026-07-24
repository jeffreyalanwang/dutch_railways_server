package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "place")
@Inheritance(strategy = InheritanceType.JOINED)
class Place (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int,

    @Column(length = 128)
    var name: String,
) {
    @ManyToMany(mappedBy = "contains")
    val locatedIn: MutableSet<Area>? = null
}