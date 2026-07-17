package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.*
import org.geolatte.geom.G2D
import org.geolatte.geom.MultiPolygon

@Entity
@Table(name = "area")
open class Area : Place() {
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    open var place: Place? = null

    @Column(name = "geom", columnDefinition = "geometry not null")
    open var geom: MultiPolygon<G2D>? = null

    @OneToMany(mappedBy = "locatedIn")
    open var contains: MutableSet<Place> = mutableSetOf()
}