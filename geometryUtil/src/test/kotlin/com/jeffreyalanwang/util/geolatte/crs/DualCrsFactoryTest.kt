package com.jeffreyalanwang.util.geolatte.com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestData
import com.jeffreyalanwang.util.geolatte.crs.DualCrsFactory
import com.jeffreyalanwang.util.geolatte.crs.assertNear
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

class DualCrsFactoryTest {

    private val factory = CoordinateTransformFactory()

    @ParameterizedTest
    @ArgumentsSource(CrsConversionTestData.ProjCoordinates::class)
    fun `Test DualCrsFactory with proj4j transform`(c2d: ProjCoordinate, g2d: ProjCoordinate) {
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
}