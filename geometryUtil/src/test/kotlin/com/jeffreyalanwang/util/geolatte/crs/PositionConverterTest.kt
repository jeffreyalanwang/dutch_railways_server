package com.jeffreyalanwang.util.geolatte.com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestData
import com.jeffreyalanwang.util.geolatte.crs.DualCrsFactory
import com.jeffreyalanwang.util.geolatte.crs.PositionConverter
import com.jeffreyalanwang.util.geolatte.crs.assertNear
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class PositionConverterTest {

    @ParameterizedTest
    @ArgumentsSource(CrsConversionTestData.Positions::class)
    fun `Test delegate PositionConverter`(c2d: C2D, g2d: G2D) {
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

}