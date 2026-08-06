@file:Suppress("ClassName")

package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.api.util.mapPositionSequence
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.geolatte.geom.*
import org.locationtech.proj4j.ProjCoordinate
import org.geolatte.geom.crs.CoordinateReferenceSystem as GeoLatteCoordinateReferenceSystem
import org.geolatte.geom.crs.CoordinateReferenceSystems as GeoLatteCoordinateReferenceSystems
import org.geolatte.geom.crs.CrsRegistry as GeoLatteCrsRegistry
import org.geolatte.geom.crs.ProjectedCoordinateReferenceSystem as GeoLatteProjectedCoordinateReferenceSystem
import org.locationtech.proj4j.CRSFactory as Proj4jCRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem as Proj4jCoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransform as Proj4jCoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory as Proj4jCoordinateTransformFactory

internal val EPSG_28992_POSITION_CONVERTER = PositionConverterFromG2D.toEpsg(28992)

/**
 * Convert GeoLatte [Position]s using intermediate proj4j [Proj4jCoordinateReferenceSystem]s.
 *
 * Not thread-safe.
 */
internal class PositionConverter<P1: Position, P2: Position>(
    proj4jCrs1: Proj4jCoordinateReferenceSystem,
    proj4jCrs2: Proj4jCoordinateReferenceSystem,
    private val constructP1: PositionConstructor<P1>,
    private val constructP2: PositionConstructor<P2>,
) {
    private val transform1to2 = factory.createTransform(proj4jCrs1, proj4jCrs2)
    private val transform2to1 = factory.createTransform(proj4jCrs2, proj4jCrs1)

    fun P1.toP2() = transform(transform1to2, constructP2)
    fun P2.toP1() = transform(transform2to1, constructP1)

    companion object {
        typealias PositionConstructor<P> = (x: Double, y: Double, z: Double) -> P

        private val factory = Proj4jCoordinateTransformFactory()
        private fun <P: Position> P.toProjCoordinate() = ProjCoordinate(
            getCoordinate(0),
            getCoordinate(1),
            if (coordinateDimension > 2) getCoordinate(2) else Double.NaN,
        )

        private inline fun <P1: Position, P2: Position> P1.transform(
            transform: Proj4jCoordinateTransform,
            constructP2: PositionConstructor<P2>,
            tempDest: () -> ProjCoordinate = { ProjCoordinate() },
        ) = toProjCoordinate()
            .let { transform.transform(it, tempDest()) }
            .run { constructP2(x, y, z) }
    }
}

/**
 * Manages both GeoLatte and proj4j coordinate reference systems.
 */
internal object DualCrsFactory {
    private val proj4j = Proj4jCRSFactory()

    fun getGeoLatteFromEPSG(epsgCode: Int): GeoLatteProjectedCoordinateReferenceSystem =
        GeoLatteCrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(epsgCode)

    fun getProj4jFromEPSG(epsgCode: Int): Proj4jCoordinateReferenceSystem =
        proj4j.createFromName("epsg:$epsgCode")

    /**
     * Get both the GeoLatte and the proj4j coordinate reference systems.
     */
    fun getCrsPairFromEPSG(epsgCode: Int) = Pair(
        getGeoLatteFromEPSG(epsgCode),
        getProj4jFromEPSG(epsgCode),
    )

    val WGS84 = GeoLatteCoordinateReferenceSystems.WGS84 to getProj4jFromEPSG(4326)
}



@Suppress("PropertyName", "LocalVariableName")
internal class PositionConverterFromG2D private constructor(
    val C2D_CRS: GeoLatteCoordinateReferenceSystem<C2D>,
    C2D_CRS_proj4j: Proj4jCoordinateReferenceSystem,
) {
    val G2D_CRS: GeoLatteCoordinateReferenceSystem<G2D> = DualCrsFactory.WGS84.first
    private val G2D_CRS_proj4j get() = DualCrsFactory.WGS84.second

    private val converter = PositionConverter(G2D_CRS_proj4j, C2D_CRS_proj4j, ::G2D, ::C2D)

    fun G2D.toC2D() = converter.run { toP2() }
    fun C2D.toG2D() = converter.run { toP1() }

    companion object {
        private fun C2D(x: Double, y: Double, z: Double) = C2D(x, y)
        private fun G2D(x: Double, y: Double, z: Double) = G2D(x, y)

        fun toEpsg(epsgCode: Int) =
            DualCrsFactory.getCrsPairFromEPSG(epsgCode)
                .run { PositionConverterFromG2D(first, second) }
    }
}

/**
 * JPA converter facilitating entries in G2D/WGS84/LonLat coordinate system,
 * with an arbitrary coordinate system in-database via [positionConverter].
 */
@Converter
internal open class PointConverterFromG2D(
    private val positionConverter: PositionConverterFromG2D,
): AttributeConverter<Point<G2D>, Point<C2D>> {

    override fun convertToDatabaseColumn(attribute: Point<G2D>?) = with (positionConverter) {
        attribute?.convertTo(C2D_CRS) { it.toC2D() }
    }

    override fun convertToEntityAttribute(dbData: Point<C2D>?) = with (positionConverter) {
        dbData?.convertTo(G2D_CRS) { it.toG2D() }
    }
}

/**
 * JPA converter facilitating entries in G2D/WGS84/LonLat coordinate system,
 * with an arbitrary coordinate system in-database via [positionConverter].
 */
@Converter
internal open class MultiPolygonConverterFromG2D(
    private val positionConverter: PositionConverterFromG2D,
): AttributeConverter<MultiPolygon<G2D>, MultiPolygon<C2D>> {

    override fun convertToDatabaseColumn(attribute: MultiPolygon<G2D>?) = with (positionConverter) {
        attribute?.convertTo(C2D_CRS) { it.toC2D() }
    }

    override fun convertToEntityAttribute(dbData: MultiPolygon<C2D>?) = with (positionConverter) {
        dbData?.convertTo(G2D_CRS) { it.toG2D() }
    }

}

private fun <S: Position, T: Position> Point<S>.convertTo(crs: GeoLatteCoordinateReferenceSystem<T>, block: (S) -> T) =
    block(position)
        .let { Point(it, crs) }

private inline fun <S: Position, reified T: Position> LineString<S>.convertTo(crs: GeoLatteCoordinateReferenceSystem<T>, block: (S) -> T) =
    positions.mapPositionSequence { block(it) }
        .let { LineString(it, crs) }

private inline fun <S: Position, reified T: Position> LinearRing<S>.convertTo(crs: GeoLatteCoordinateReferenceSystem<T>, block: (S) -> T) =
    (this as LineString<S>).convertTo(crs, block)
        .let { LinearRing(it) }

private inline fun <S: Position, reified T: Position> Polygon<S>.convertTo(crs: GeoLatteCoordinateReferenceSystem<T>, block: (S) -> T) =
    map { linearRing -> linearRing.convertTo(crs, block) }
        .let { rings -> Polygon(*rings.toTypedArray()) }

private inline fun <S: Position, reified T: Position> MultiPolygon<S>.convertTo(crs: GeoLatteCoordinateReferenceSystem<T>, block: (S) -> T) =
    map { polygon -> polygon.convertTo(crs, block) }
        .let { polygons -> MultiPolygon(*polygons.toTypedArray()) }