package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "passservice")
class PassService(

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id var id: Int = -1,

    @Column(length = 128)
    var name: String = "",

) {

    @ManyToOne(optional = false)
    @JoinColumn(name = "consist", nullable = false)
    var consist: TrainsetType? =
        null

    @OneToMany(mappedBy = "service")
    val stops: MutableSet<Stop> =
        mutableSetOf()

}