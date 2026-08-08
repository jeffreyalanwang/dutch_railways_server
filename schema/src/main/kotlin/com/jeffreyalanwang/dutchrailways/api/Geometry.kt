package com.jeffreyalanwang.dutchrailways.api

import com.jeffreyalanwang.util.geolatte.mapToMultiPolygon
import com.jeffreyalanwang.util.geolatte.mapToPolygon
import com.jeffreyalanwang.util.geolatte.mapToPositionSequence
import org.geolatte.geom.*
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

public data class GeoCoords(
    val latitude: Double,
    val longitude: Double,
) {
    public constructor(obj: G2D) : this(latitude = obj.lat, longitude = obj.lon)
    public constructor(obj: Point<G2D>) : this(obj.position)

    public fun toGeoLattePosition(): G2D = G2D(longitude, latitude)
    public fun toGeoLatte(): Point<G2D> = Point(toGeoLattePosition(), WGS84)
}

public data class GeoLinearRing(
    val points: List<GeoCoords>
) {
    public constructor(obj: LinearRing<G2D>) : this(obj.positions.map { GeoCoords(it) })
    public fun toGeoLatte(): LinearRing<G2D> = LinearRing(
        points.mapToPositionSequence { it.toGeoLattePosition() },
        WGS84,
    )
}

public data class GeoPolygon(
    val rings: List<GeoLinearRing>
) {
    public constructor(obj: Polygon<G2D>) : this(obj.map { GeoLinearRing(it) })
    public fun toGeoLatte(): Polygon<G2D> = rings.mapToPolygon { it.toGeoLatte() }
}

public data class GeoMultiPolygon(
    val polygons: List<GeoPolygon>
) {
    public constructor(obj: MultiPolygon<G2D>) : this(obj.map { GeoPolygon(it) })
    public fun toGeoLatte(): MultiPolygon<G2D> = polygons.mapToMultiPolygon { it.toGeoLatte() }
}

public data class GeoRect(
    val northwest: GeoCoords,
    val southeast: GeoCoords,
)