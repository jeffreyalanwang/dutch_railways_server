package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "station")
class Station(

    id: Int = -1,
    name: String = "",

    @Column(length = 256)
    var address: String = "",

//    @Column(columnDefinition = "geometry not null")
//    var geom: Point<G2D>? = null,

) : Place(id, name) {

    @OneToMany(mappedBy = "station")
    val stops: MutableSet<Stop>? = null

}