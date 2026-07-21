package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "area")
class Area(
    id: Int = -1,
    name: String = "",

//    @Column(columnDefinition = "geometry not null")
//    var geom: MultiPolygon<G2D>,

) : Place(id, name) {

    @OneToMany(mappedBy = "locatedIn")
    var contains: MutableSet<Place> = mutableSetOf()

}