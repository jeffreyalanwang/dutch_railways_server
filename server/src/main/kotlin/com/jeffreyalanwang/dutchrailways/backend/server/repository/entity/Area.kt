package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import com.jeffreyalanwang.dutchrailways.backend.server.repository.EPSG_28992_POSITION_CONVERTER
import com.jeffreyalanwang.dutchrailways.backend.server.repository.MultiPolygonConverterFromG2D
import jakarta.persistence.*
import org.geolatte.geom.G2D
import org.geolatte.geom.MultiPolygon
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed

@Converter
private class MultiPolygonConverterEpsg28992: MultiPolygonConverterFromG2D(EPSG_28992_POSITION_CONVERTER)

@Indexed
@Entity
@Table(name = "area")
class Area(
    id: Int = -1,
    name: String = "",

    @Column(columnDefinition = "geometry(Point, 28992) not null")
    @Convert(MultiPolygonConverterEpsg28992::class)
    var geom: MultiPolygon<G2D>? = null,
) : Place(id, name) {

    @ManyToMany
    @JoinTable(
        name = "placehierarchy",
        joinColumns = [JoinColumn(name = "parent", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "child", referencedColumnName = "id")],
    )
    @Column(updatable = false)
    val contains: MutableSet<Place> = mutableSetOf()

}