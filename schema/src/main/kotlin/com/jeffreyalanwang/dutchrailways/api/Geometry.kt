package com.jeffreyalanwang.dutchrailways.api.util

import org.geolatte.geom.*
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

data class GeoCoords(
    val latitude: Double,
    val longitude: Double,
): Point<G2D>(
    G2D(longitude, latitude),
    WGS84,
) {
    constructor(obj: G2D) : this(latitude = obj.lat, longitude = obj.lon)
    constructor(obj: Point<G2D>) : this(obj.position)
}

data class GeoLinearRing(
    val points: List<GeoCoords>
): LinearRing<G2D>(
    PositionSequence<_, G2D>(points) { it.position },
    WGS84,
) {
    constructor(obj: LinearRing<G2D>) : this(obj.positions.map { GeoCoords(it) })
}

data class GeoPolygon(
    val rings: List<GeoLinearRing>
): Polygon<G2D>(
    *rings.toTypedArray(),
) {
    constructor(obj: Polygon<G2D>) : this(obj.map { GeoLinearRing(it) })
}

data class GeoMultiPolygon(
    val polygons: List<GeoPolygon>
): MultiPolygon<G2D>(
    *polygons.toTypedArray()
) {
    constructor(obj: MultiPolygon<G2D>) : this(obj.map { GeoPolygon(it) })
}
