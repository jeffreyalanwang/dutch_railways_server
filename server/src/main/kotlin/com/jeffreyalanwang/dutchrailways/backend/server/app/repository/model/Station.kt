package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.*
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

@Entity
@Table(name = "station")
open class Station : Place() {
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    open var place: Place? = null

    @Column(name = "address", length = 256)
    open var address: String? = null

    @Column(name = "geom", columnDefinition = "geometry not null")
    open var geom: Point<G2D>? = null

    @OneToMany
    @JoinColumn(name = "station")
    open var stops: MutableSet<Stop> = mutableSetOf()
}