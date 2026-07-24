package com.jeffreyalanwang.dutchrailways.backend.server.dto

import org.geolatte.geom.*

/**
 * For consumption by GraphQL schema.
 */
interface GeoCoords {
    val latitude: Double
    val longitude: Double
}

class GeoLatteGeoCoords(private val obj: G2D) : GeoCoords {
    constructor(obj: Point<G2D>) : this(obj.position)
    override val latitude get() = obj.lat
    override val longitude get() = obj.lon
}

interface GeoLinearRing {
    val points: List<GeoCoords>
}

class GeoLatteGeoLinearRing(private val obj: LinearRing<G2D>): GeoLinearRing {
    override val points get() = obj.positions.map { GeoLatteGeoCoords(it) }
}

interface GeoPolygon {
    val rings: List<GeoLinearRing>
}

class GeoLatteGeoPolygon(private val obj: Polygon<G2D>): GeoPolygon {
    override val rings get() = obj.map { GeoLatteGeoLinearRing(it) }
}

interface GeoMultiPolygon {
    val polygons: List<GeoPolygon>
}

class GeoLatteGeoMultiPolygon(private val obj: MultiPolygon<G2D>): GeoMultiPolygon {
    override val polygons get() = obj.map { GeoLatteGeoPolygon(it) }
}
