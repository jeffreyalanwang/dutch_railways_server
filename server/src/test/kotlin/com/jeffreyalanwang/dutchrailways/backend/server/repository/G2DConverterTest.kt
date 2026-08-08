package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestData
import com.jeffreyalanwang.util.geolatte.crs.assertNear
import jakarta.persistence.AttributeConverter
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.test.Test

class G2DConverterTest {
    val positionConverter = EPSG_28992_POSITION_CONVERTER
        .also {
            it.C2D_CRS.crsId.run {
                assertEquals("EPSG", authority)
                assertEquals(CrsConversionTestData.epsgCode, code)
            }
        }

    @Nested
    @ParameterizedClass
    @ArgumentsSource(CrsConversionTestData.Points::class)
    inner class PointConverterTest(
        private val c2d: Point<C2D>,
        private val g2d: Point<G2D>,
    ) {
        private val pointConverter: AttributeConverter<Point<G2D>, Point<C2D>> =
            PointConverterFromG2D(positionConverter)

        @Test
        fun `Get entity attribute (toG2D)`() {
            val result = pointConverter.convertToEntityAttribute(c2d)
            assertNear(g2d, result!!)
        }

        @Test
        fun `Get database value (toC2D)`() {
            val result = pointConverter.convertToDatabaseColumn(g2d)
            assertNear(c2d, result!!)
        }
    }

    @Nested
    @ParameterizedClass
    @ArgumentsSource(CrsConversionTestData.MultiPolygons::class)
    inner class MultiPolygonConverterTest(
        private val c2d: MultiPolygon<C2D>,
        private val g2d: MultiPolygon<G2D>,
    ) {
        private val multiPolygonConverter: AttributeConverter<MultiPolygon<G2D>, MultiPolygon<C2D>> =
            MultiPolygonConverterFromG2D(positionConverter)

        @Test
        fun `Get entity attribute (toG2D)`() {
            val result = multiPolygonConverter.convertToEntityAttribute(c2d)
            assertNear(g2d, result!!)
        }

        @Test
        fun `Get database value (toC2D)`() {
            val result = multiPolygonConverter.convertToDatabaseColumn(g2d)
            assertNear(c2d, result!!)
        }
    }

}