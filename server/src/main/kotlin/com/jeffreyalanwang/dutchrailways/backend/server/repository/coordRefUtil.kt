@file:Suppress("ClassName")

package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.api.util.buildPositionSequence
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.geolatte.geom.*
import org.geolatte.geom.crs.CoordinateReferenceSystem
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.geolatte.geom.crs.CrsRegistry
import org.geolatte.geom.crs.trans.CoordinateOperations.transform

internal val EPSG_28992_POSITION_CONVERTER = PositionConverterToEPSGFromG2D(28992)

internal fun PositionConverterToEPSGFromG2D(epsgCode: Int) =
    CrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(epsgCode)
        .let { PositionConverterFromG2D(it) }

internal class PositionConverterFromG2D(
    val C2D_CRS: CoordinateReferenceSystem<C2D>
) {
    val G2D_CRS = WGS84

    private val C2D_TO_G2D = transform(C2D_CRS.crsId, G2D_CRS.crsId)

    fun G2D.toC2D() = toCoords(::C2D, destDimension = 2, C2D_TO_G2D::reverse)

    fun C2D.toG2D() = toCoords(::G2D, destDimension = 2, C2D_TO_G2D::forward)

    private companion object {
        fun C2D(arr: DoubleArray) = C2D(arr[0], arr[1])
        fun G2D(arr: DoubleArray) = G2D(arr[0], arr[1])
        fun <T> Position.toCoords(destConstructorFromArr: (DoubleArray) -> T, destDimension: Int, transform: (DoubleArray, DoubleArray) -> Unit): T {
            val sourceArray = toArray(null)
            val destArray = DoubleArray(destDimension)
            transform(sourceArray, destArray)
            return destConstructorFromArr(destArray)
        }
    }
}

internal inline fun <S: Position, reified T: Position> PositionSequence<S>.convertTo(block: (S) -> T): PositionSequence<T> =
    buildPositionSequence(size()) {
        forEach {
            add(block(it))
        }
    }

internal fun <S: Position, T: Position> Point<S>.convertTo(crs: CoordinateReferenceSystem<T>, block: (S) -> T) =
    block(position)
        .let { Point(it, crs) }

internal inline fun <S: Position, reified T: Position> LineString<S>.convertTo(crs: CoordinateReferenceSystem<T>, block: (S) -> T) =
    positions.convertTo(block)
        .let { LineString(it, crs) }

internal inline fun <S: Position, reified T: Position> LinearRing<S>.convertTo(crs: CoordinateReferenceSystem<T>, block: (S) -> T) =
    (this as LineString<S>).convertTo(crs, block)
        .let { LinearRing(it) }

internal inline fun <S: Position, reified T: Position> Polygon<S>.convertTo(crs: CoordinateReferenceSystem<T>, block: (S) -> T) =
    map { linearRing -> linearRing.convertTo(crs, block) }
        .let { rings -> Polygon(*rings.toTypedArray()) }

internal inline fun <S: Position, reified T: Position> MultiPolygon<S>.convertTo(crs: CoordinateReferenceSystem<T>, block: (S) -> T) =
    map { polygon -> polygon.convertTo(crs, block) }
        .let { polygons -> MultiPolygon(*polygons.toTypedArray()) }

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

