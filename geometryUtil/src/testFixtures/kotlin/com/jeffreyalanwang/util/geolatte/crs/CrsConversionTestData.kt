package com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestValue.Companion.map
import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestValue.Companion.testPointsOf
import com.jeffreyalanwang.util.geolatte.mapToClosedLinearRing
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.geolatte.geom.LinearRing
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.Point
import org.geolatte.geom.Polygon
import org.geolatte.geom.crs.CoordinateReferenceSystem
import org.locationtech.proj4j.ProjCoordinate

object CrsConversionTestData {
    val epsgCode = 28992
    val C2D_CRS: CoordinateReferenceSystem<C2D> = DualCrsFactory.getGeoLatteFromEPSG(epsgCode = epsgCode)
    val G2D_CRS: CoordinateReferenceSystem<G2D> = DualCrsFactory.WGS84.first

    private val list = testPointsOf(
        C2D(10.0, 15.0)                             to G2D(3.3136858, 47.974903),
        C2D(-5795218.39379561, 2784077.663599484)   to G2D(-74.0445, 40.6892),
        C2D(-72062.57907743842, 100934.63046100503) to G2D(2.2945, 48.8584),
        C2D(42684530.823743485, 19199832.631670307) to G2D(151.2153, -33.8568),
        C2D(2718938.5811047046, -1638240.759423877) to G2D(31.1342, 29.9792),
        C2D(7268274.277077766, 1066807.299353912)   to G2D(78.0421, 27.1751),
    )

    val c2d get() = list.map { it.c2d }
    val g2d get() = list.map { it.g2d }

    val positions get() = list
    val projCoordinates = list.map { testValue ->
        testValue.map { p ->
            p.toProjCoordinate()
        }
    }
    val points = list.map { testValue ->
        CrsConversionTestValue(
            c2d = Point(testValue.c2d, C2D_CRS),
            g2d = Point(testValue.g2d, G2D_CRS),
        )
    }
    val linearRings = listOf(
        CrsConversionTestValue(
            c2d = c2d.mapToClosedLinearRing(C2D_CRS) { it },
            g2d = g2d.mapToClosedLinearRing(G2D_CRS) { it },
        ),
    )
    val polygons = linearRings.map { testValue ->
        CrsConversionTestValue(
            c2d = Polygon(testValue.c2d),
            g2d = Polygon(testValue.g2d),
        )
    }
    val multiPolygons = polygons.map { testValue ->
        CrsConversionTestValue(
            c2d = MultiPolygon(testValue.c2d),
            g2d = MultiPolygon(testValue.g2d),
        )
    }

    class Positions : CrsConversionTestDataProvider<C2D, G2D>() {
        override fun provideTestValues() = positions.stream()
    }
    class ProjCoordinates : CrsConversionTestDataProvider<ProjCoordinate, ProjCoordinate>() {
        override fun provideTestValues() = projCoordinates.stream()
    }
    class Points : CrsConversionTestDataProvider<Point<C2D>, Point<G2D>>() {
        override fun provideTestValues() = points.stream()
    }
    class LinearRings : CrsConversionTestDataProvider<LinearRing<C2D>, LinearRing<G2D>>() {
        override fun provideTestValues() = linearRings.stream()
    }
    class Polygons : CrsConversionTestDataProvider<Polygon<C2D>, Polygon<G2D>>() {
        override fun provideTestValues() = polygons.stream()
    }
    class MultiPolygons : CrsConversionTestDataProvider<MultiPolygon<C2D>, MultiPolygon<G2D>>() {
        override fun provideTestValues() = multiPolygons.stream()
    }
}
