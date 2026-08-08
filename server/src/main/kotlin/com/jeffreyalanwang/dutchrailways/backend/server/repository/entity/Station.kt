package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import com.jeffreyalanwang.dutchrailways.backend.server.repository.EPSG_28992_POSITION_CONVERTER
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PointConverterFromG2D
import jakarta.persistence.*
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.jetbrains.annotations.VisibleForTesting

@Converter
private class LatLngConverterEpsg28992: PointConverterFromG2D(EPSG_28992_POSITION_CONVERTER)

@GeoPointBinding(fieldName = "geom")
@Indexed
@Entity
@Table(name = "station")
class Station(

    id: Int = -1,
    name: String = "",

    @field:FullTextField
    @Column(length = 256)
    var address: String = "",

    @Column(columnDefinition = "geometry(Point, 28992) not null")
    @Convert(LatLngConverterEpsg28992::class)
    var geom: Point<G2D>? = null,

) : Place(id, name) {

    @OneToMany(mappedBy = "station")
    val stops: MutableSet<Stop>? = null

    @get:Latitude
    @get:Transient
    @get:IndexingDependency(derivedFrom = [ObjectPath(PropertyValue("geom"))])
    val lat get() = geom?.run { position.lat }

    @get:Longitude
    @get:Transient
    @get:IndexingDependency(derivedFrom = [ObjectPath(PropertyValue("geom"))])
    val lon get() = geom?.run { position.lon }

}