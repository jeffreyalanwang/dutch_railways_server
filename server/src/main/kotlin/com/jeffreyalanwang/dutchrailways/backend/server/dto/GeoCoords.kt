package com.jeffreyalanwang.dutchrailways.backend.server.dto

import com.jeffreyalanwang.dutchrailways.backend.server.dto.GeoLatteGeoCoords.Companion.toGeoLatte
import com.jeffreyalanwang.dutchrailways.backend.server.dto.GeoLatteGeoLinearRing.Companion.toGeoLatte
import com.jeffreyalanwang.dutchrailways.backend.server.dto.GeoLatteGeoPolygon.Companion.toGeoLatte
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PositionSequence
import org.geolatte.geom.*
import org.geolatte.geom.crs.CoordinateReferenceSystem
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

/**
 * Interfaces in this file correspond to GraphQL input/output.
 */

interface GeoCoords {
    val latitude: Double
    val longitude: Double
}

class GeoLatteGeoCoords(private val obj: G2D) : GeoCoords {

    override val latitude get() = obj.lat
    override val longitude get() = obj.lon

    constructor(obj: Point<G2D>) : this(obj.position)
    companion object {
        fun GeoCoords.toGeoLatte() = if (this is GeoLatteGeoCoords) obj else G2D(longitude, latitude)
        fun G2D.toPoint(crs: CoordinateReferenceSystem<G2D> = WGS84) = Point(this, crs)
    }
}

interface GeoLinearRing {
    val points: List<GeoCoords>
}

class GeoLatteGeoLinearRing(private val obj: LinearRing<G2D>): GeoLinearRing {
    override val points get() = obj.positions.map { GeoLatteGeoCoords(it) }

    companion object {
        fun GeoLinearRing.toGeoLatte(crs: CoordinateReferenceSystem<G2D> = WGS84) =
            if (this is GeoLatteGeoLinearRing) obj
            else LinearRing(
                PositionSequence(points) { it.toGeoLatte() },
                crs,
            )
    }
}

interface GeoPolygon {
    val rings: List<GeoLinearRing>
}

class GeoLatteGeoPolygon(private val obj: Polygon<G2D>): GeoPolygon {
    override val rings get() = obj.map { GeoLatteGeoLinearRing(it) }

    companion object {
        fun GeoPolygon.toGeoLatte(crs: CoordinateReferenceSystem<G2D> = WGS84) =
            if (this is GeoLatteGeoPolygon) obj
            else Polygon(
                *rings.map { it.toGeoLatte(crs) }.toTypedArray(),
            )
    }
}

interface GeoMultiPolygon {
    val polygons: List<GeoPolygon>
}

class GeoLatteGeoMultiPolygon(private val obj: MultiPolygon<G2D>): GeoMultiPolygon {
    override val polygons get() = obj.map { GeoLatteGeoPolygon(it) }

    companion object {
        fun GeoMultiPolygon.toGeoLatte(crs: CoordinateReferenceSystem<G2D> = WGS84) =
            if (this is GeoLatteGeoMultiPolygon) obj
            else MultiPolygon(
                *polygons.map { it.toGeoLatte(crs) }.toTypedArray(),
            )
    }
}
