@file:Suppress("ClassName")

package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.util.geolatte.crs.CrsConverterToG2D
import com.jeffreyalanwang.util.geolatte.crs.fromEpsg
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.geolatte.geom.*

internal val EPSG_28992_POSITION_CONVERTER = CrsConverterToG2D.fromEpsg(28992)

/**
 * JPA converter facilitating entries in G2D/WGS84/LonLat coordinate system,
 * with an arbitrary coordinate system in-database via [converter].
 */
@Converter
internal open class PointConverterFromG2D(
    private val converter: CrsConverterToG2D,
): AttributeConverter<Point<G2D>, Point<C2D>>, CrsConverterToG2D by converter {

    override fun convertToDatabaseColumn(attribute: Point<G2D>?) = attribute?.toC2D()
    override fun convertToEntityAttribute(dbData: Point<C2D>?) = dbData?.toG2D()
}

/**
 * JPA converter facilitating entries in G2D/WGS84/LonLat coordinate system,
 * with an arbitrary coordinate system in-database via [converter].
 */
@Converter
internal open class MultiPolygonConverterFromG2D(
    private val converter: CrsConverterToG2D,
): AttributeConverter<MultiPolygon<G2D>, MultiPolygon<C2D>>, CrsConverterToG2D by converter {

    override fun convertToDatabaseColumn(attribute: MultiPolygon<G2D>?) = attribute?.toC2D()
    override fun convertToEntityAttribute(dbData: MultiPolygon<C2D>?) = dbData?.toG2D()
}
