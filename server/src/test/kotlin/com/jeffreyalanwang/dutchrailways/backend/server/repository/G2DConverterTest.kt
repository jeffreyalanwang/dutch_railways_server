package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.api.util.PositionSequence
import org.geolatte.geom.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.locationtech.proj4j.ProjCoordinate
import kotlin.test.Test
import org.locationtech.proj4j.CoordinateTransformFactory as Proj4jCoordinateTransformFactory

private const val CONVERSION_ERROR = 0.5 // Though this is large for WGS84, it is small for EPSG28992

private fun assertNear(expected: ProjCoordinate, actual: ProjCoordinate) {
    assertEquals(expected.x, actual.x, CONVERSION_ERROR)
    assertEquals(expected.y, actual.y, CONVERSION_ERROR)
//    assertEquals(expected.z, actual.z, CONVERSION_ERROR) // we do not use this and it is appearing to produce arbitrary values
}

private fun assertNear(expected: Position, actual: Position) {
    assertEquals(expected.coordinateDimension, actual.coordinateDimension)
    for (i in 0 ..< expected.coordinateDimension) {
        assertEquals(expected.getCoordinate(i), actual.getCoordinate(i), CONVERSION_ERROR)
    }
}

private fun assertNotNear(expected: Position, actual: Position) {
    assertEquals(expected.coordinateDimension, actual.coordinateDimension)
    for (i in 0 ..< expected.coordinateDimension) {
        assertNotEquals(expected.getCoordinate(i), actual.getCoordinate(i), CONVERSION_ERROR)
    }
}

private fun <P : Position> assertNear(expected: Geometry<P>, actual: Geometry<P>) {
    assertEquals(expected.geometryType, actual.geometryType)
    assertEquals(expected.coordinateReferenceSystem, actual.coordinateReferenceSystem)
    assertEquals(expected.numPositions, actual.numPositions)
    for (i in 0 ..< expected.numPositions) {
        assertNear(expected.getPositionN(i), actual.getPositionN(i))
    }
}

class G2DConverterTest {
    private val positionConverter = EPSG_28992_POSITION_CONVERTER
    private val pointConverter = PointConverterFromG2D(positionConverter)
    private val multiPolygonConverter = MultiPolygonConverterFromG2D(positionConverter)

    private object TestData {
        val c2d = listOf(
            C2D(10.0, 15.0),
            C2D(-5795218.39379561, 2784077.663599484),
            C2D(-72062.57907743842, 100934.63046100503),
            C2D(42684530.823743485, 19199832.631670307),
            C2D(2718938.5811047046, -1638240.759423877),
            C2D(7268274.277077766, 1066807.299353912),
        )
        val g2d = listOf(
            G2D(3.3136858, 47.974903),
            G2D(-74.0445, 40.6892),
            G2D(2.2945, 48.8584),
            G2D(151.2153, -33.8568),
            G2D(31.1342, 29.9792),
            G2D(78.0421, 27.1751),
        )
    }

    @Test
    fun `Test DualCrsFactory with proj4j transform`() {
        val c2d = TestData.c2d[0].run { ProjCoordinate(x, y) }
        val g2d = TestData.g2d[0].run { ProjCoordinate(lon, lat) }

        val factory = Proj4jCoordinateTransformFactory()
        val gToC = factory.createTransform(
            DualCrsFactory.WGS84.second,
            DualCrsFactory.getProj4jFromEPSG(28992),
        )
        val cToG = factory.createTransform(
            DualCrsFactory.getProj4jFromEPSG(28992),
            DualCrsFactory.WGS84.second,
        )

        assertNear(c2d, gToC.transform(g2d, ProjCoordinate()))
        assertNear(g2d, cToG.transform(c2d, ProjCoordinate()))
    }

    @Test
    fun `Test delegate PositionConverter`() {
        val c2d = TestData.c2d[0]
        val g2d = TestData.g2d[0]

        val converter = PositionConverter(
            DualCrsFactory.WGS84.second,
            DualCrsFactory.getProj4jFromEPSG(28992),
            { x, y, z -> G2D(x, y) },
            { x, y, z -> C2D(x, y) },
        )

        converter.run {
            assertNear(g2d, c2d.toP1())
            assertNear(c2d, g2d.toP2())
        }
    }

    @Nested
    inner class PositionConverterTest {
        private val c2d = TestData.c2d[0]
        private val g2d = TestData.g2d[0]

        @Test
        fun toG2D() {
            val result = positionConverter.run { c2d.toG2D() }
            assertNear(g2d, result)
        }

        @Test
        fun toC2D() {
            val result = positionConverter.run { g2d.toC2D() }
            assertNear(c2d, result)
        }

        @Test
        fun `Check that conversion is not backwards`() {
            val c2d = this.g2d.run { C2D(lon, lat) }
            val g2d = this.c2d.run { G2D(x, y) }

            positionConverter.run {
                assertNotNear(g2d, c2d.toG2D())
                assertNotNear(c2d, g2d.toC2D())
            }
        }

        @Test
        fun `Check that conversion is not silently failing with identity operation`() {
            positionConverter.run {
                assertNotNear(g2d.run { C2D(lon, lat) }, g2d.toC2D())
                assertNotNear(c2d.run { G2D(x, y) }, c2d.toG2D())
            }
        }
    }

    @Nested
    inner class PointConverterTest {
        private val c2d = Point(TestData.c2d[0], positionConverter.C2D_CRS)
        private val g2d = Point(TestData.g2d[0], positionConverter.G2D_CRS)

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
    inner class MultiPolygonConverterTest {
        private val c2d = TestData.c2d
            .run { this + first() }
            .let { PositionSequence(it) }
            .let { Polygon(it, positionConverter.C2D_CRS) }
            .let { MultiPolygon(it) }

        private val g2d = TestData.g2d
            .run { this + first() }
            .let { PositionSequence(it) }
            .let { Polygon(it, positionConverter.G2D_CRS) }
            .let { MultiPolygon(it) }

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