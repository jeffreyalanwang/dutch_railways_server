@file:Suppress("ClassName", "PropertyName", "LocalVariableName", "PrivatePropertyName")

package com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.mapToMultiPolygon
import com.jeffreyalanwang.util.geolatte.mapToPolygon
import com.jeffreyalanwang.util.geolatte.mapToPositionSequence
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.LinearRing
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.Point
import org.geolatte.geom.Polygon
import org.geolatte.geom.PositionSequence
import org.geolatte.geom.crs.CoordinateReferenceSystem as GeoLatteCrs
import org.locationtech.proj4j.CoordinateReferenceSystem as Proj4jCrs

public interface CrsConverterToG2D {
    public companion object;

    public val G2D_CRS: GeoLatteCrs<G2D>
    public val C2D_CRS: GeoLatteCrs<C2D>
    public fun G2D.toC2D(): C2D
    public fun C2D.toG2D(): G2D

    public fun PositionSequence<G2D>.toC2D(): PositionSequence<C2D> = mapToPositionSequence { it.toC2D() }
    public fun PositionSequence<C2D>.toG2D(): PositionSequence<G2D> = mapToPositionSequence { it.toG2D() }

    public fun Point<G2D>.toC2D(): Point<C2D> = Point(position.toC2D(), C2D_CRS)
    public fun Point<C2D>.toG2D(): Point<G2D> = Point(position.toG2D(), G2D_CRS)
    public fun LineString<G2D>.toC2D(): LineString<C2D> = LineString(positions.toC2D(), C2D_CRS)
    public fun LineString<C2D>.toG2D(): LineString<G2D> = LineString(positions.toG2D(), G2D_CRS)

    public fun LinearRing<G2D>.toC2D(): LinearRing<C2D> = LinearRing((this as LineString<G2D>).toC2D())
    public fun LinearRing<C2D>.toG2D(): LinearRing<G2D> = LinearRing((this as LineString<C2D>).toG2D())

    public fun Polygon<G2D>.toC2D(): Polygon<C2D> = mapToPolygon { it.toC2D() }
    public fun Polygon<C2D>.toG2D(): Polygon<G2D> = mapToPolygon { it.toG2D() }
    public fun MultiPolygon<G2D>.toC2D(): MultiPolygon<C2D> = mapToMultiPolygon { it.toC2D() }
    public fun MultiPolygon<C2D>.toG2D(): MultiPolygon<G2D> = mapToMultiPolygon { it.toG2D() }
}

private class CrsConverterImpl private constructor(
    override val C2D_CRS: GeoLatteCrs<C2D>,
    C2D_CRS_proj4j: Proj4jCrs,
): CrsConverterToG2D {
    override val G2D_CRS: GeoLatteCrs<G2D> = DualCrsFactory.WGS84.first
    private val G2D_CRS_proj4j get() = DualCrsFactory.WGS84.second

    companion object {
        private fun C2D(x: Double, y: Double, z: Double) = C2D(x, y)
        private fun G2D(x: Double, y: Double, z: Double) = G2D(x, y)

        fun forEpsgC2D(epsgCode: Int): CrsConverterImpl =
            DualCrsFactory.getCrsPairFromEPSG(epsgCode)
                .run { CrsConverterImpl(first, second) }
    }

    private val converter = PositionConverter(G2D_CRS_proj4j, C2D_CRS_proj4j, ::G2D, ::C2D)
    override fun G2D.toC2D(): C2D = converter.run { toP2() }
    override fun C2D.toG2D(): G2D = converter.run { toP1() }
}

public fun CrsConverterToG2D.Companion.fromEpsg(epsgCode: Int): CrsConverterToG2D =
    CrsConverterImpl.forEpsgC2D(epsgCode)
