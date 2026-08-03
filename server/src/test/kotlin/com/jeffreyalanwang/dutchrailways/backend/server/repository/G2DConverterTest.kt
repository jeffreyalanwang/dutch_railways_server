package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.api.util.PositionSequence
import org.geolatte.geom.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class G2DConverterTest {
    private val positionConverter = EPSG_28992_POSITION_CONVERTER
    private val pointConverter = PointConverterFromG2D(positionConverter)
    private val multiPolygonConverter = MultiPolygonConverterFromG2D(positionConverter)

    private object TestData {
        val c2d = listOf(
            C2D(-5795218.39379561, 2784077.663599484),
            C2D(-72062.57907743842, 100934.63046100503),
            C2D(42684530.823743485, 19199832.631670307),
            C2D(2718938.5811047046, -1638240.759423877),
            C2D(7268274.277077766, 1066807.299353912),
        )
        val g2d = listOf(
            G2D(-74.0445, 40.6892),
            G2D(2.2945, 48.8584),
            G2D(151.2153, -33.8568),
            G2D(31.1342, 29.9792),
            G2D(78.0421, 27.1751),
        )
    }

    @Nested
    inner class PositionConverterTest {
        private val c2d = TestData.c2d[0]
        private val g2d = TestData.g2d[0]

        @Test
        fun toG2D() {
            val result = positionConverter.run { c2d.toG2D() }
            assertEquals(g2d, result)
        }

        @Test
        fun toC2D() {
            val result = positionConverter.run { g2d.toC2D() }
            assertEquals(c2d, result)
        }
    }

    @Nested
    inner class PointConverterTest {
        private val c2d = Point(TestData.c2d[0], positionConverter.C2D_CRS)
        private val g2d = Point(TestData.g2d[0], positionConverter.G2D_CRS)

        @Test
        fun `Get entity attribute (toG2D)`() {
            val result = pointConverter.convertToEntityAttribute(c2d)
            assertEquals(g2d, result)
        }

        @Test
        fun `Get database value (toC2D)`() {
            val result = pointConverter.convertToDatabaseColumn(g2d)
            assertEquals(c2d, result)
        }
    }

    @Nested
    inner class MultiPolygonConverterTest {
        private val c2d = PositionSequence(TestData.c2d)
            .let { Polygon(it, positionConverter.C2D_CRS) }
            .let { MultiPolygon(it) }

        private val g2d = PositionSequence(TestData.g2d)
            .let { Polygon(it, positionConverter.G2D_CRS) }
            .let { MultiPolygon(it) }

        @Test
        fun `Get entity attribute (toG2D)`() {
            val result = multiPolygonConverter.convertToEntityAttribute(c2d)
            assertEquals(g2d, result)
        }

        @Test
        fun `Get database value (toC2D)`() {
            val result = multiPolygonConverter.convertToDatabaseColumn(g2d)
            assertEquals(c2d, result)
        }
    }

}