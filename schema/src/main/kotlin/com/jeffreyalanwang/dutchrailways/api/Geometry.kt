package com.jeffreyalanwang.dutchrailways.api

import com.jeffreyalanwang.dutchrailways.api.util.mapToMultiPolygon
import com.jeffreyalanwang.dutchrailways.api.util.mapToPolygon
import com.jeffreyalanwang.dutchrailways.api.util.mapToPositionSequence
import org.geolatte.geom.*
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

data class GeoCoords(
    val latitude: Double,
    val longitude: Double,
) {
    constructor(obj: G2D) : this(latitude = obj.lat, longitude = obj.lon)
    constructor(obj: Point<G2D>) : this(obj.position)

    fun toGeoLattePosition() = G2D(longitude, latitude)
    fun toGeoLatte() = Point(toGeoLattePosition(), WGS84)
}

data class GeoLinearRing(
    val points: List<GeoCoords>
) {
    constructor(obj: LinearRing<G2D>) : this(obj.positions.map { GeoCoords(it) })
    fun toGeoLatte() = LinearRing(
        points.mapToPositionSequence { it.toGeoLattePosition() },
        WGS84,
    )
}

data class GeoPolygon(
    val rings: List<GeoLinearRing>
) {
    constructor(obj: Polygon<G2D>) : this(obj.map { GeoLinearRing(it) })
    fun toGeoLatte() = rings.mapToPolygon { it.toGeoLatte() }
}

data class GeoMultiPolygon(
    val polygons: List<GeoPolygon>
) {
    constructor(obj: MultiPolygon<G2D>) : this(obj.map { GeoPolygon(it) })
    fun toGeoLatte() = polygons.mapToMultiPolygon { it.toGeoLatte() }
}

data class GeoRect(
    val northwest: GeoCoords,
    val southeast: GeoCoords,
)