package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "trainsettype")
open class TrainsetType {
    @Id
    @Column(name = "name", nullable = false, length = 64)
    open var name: String = ""

    @ManyToMany
    open var amenities: MutableSet<Amenity> = mutableSetOf()
}
