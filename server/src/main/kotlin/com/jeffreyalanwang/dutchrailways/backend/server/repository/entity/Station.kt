package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import com.jeffreyalanwang.dutchrailways.backend.server.repository.EPSG_28992_POSITION_CONVERTER
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PointConverterFromG2D
import jakarta.persistence.*
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

@Converter
class LatLngConverterEpsg28992: PointConverterFromG2D(EPSG_28992_POSITION_CONVERTER)

@Entity
@Table(name = "station")
class Station(

    id: Int = -1,
    name: String = "",

    @Column(length = 256)
    var address: String = "",

    @Column(columnDefinition = "geometry(Point, 28992) not null")
    @Convert(LatLngConverterEpsg28992::class)
    var geom: Point<G2D>? = null,

) : Place(id, name) {

    @OneToMany(mappedBy = "station")
    val stops: MutableSet<Stop>? = null

}