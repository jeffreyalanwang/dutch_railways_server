package com.jeffreyalanwang.util.geolatte.com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.crs.CrsConversionTestData
import com.jeffreyalanwang.util.geolatte.crs.CrsConverterToG2D
import com.jeffreyalanwang.util.geolatte.crs.assertNear
import com.jeffreyalanwang.util.geolatte.crs.fromEpsg
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.test.Test

@ParameterizedClass
@ArgumentsSource(CrsConversionTestData.Positions::class)
class CrsConverterTest(
    private val c2d: C2D,
    private val g2d: G2D,
) {
    private val positionConverter = CrsConverterToG2D.fromEpsg(CrsConversionTestData.epsgCode)

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

        assertThrows<Throwable> {
            positionConverter.run {
                assertNear(g2d, c2d.toG2D())
                assertNear(c2d, g2d.toC2D())
            }
        }
    }

    @Test
    fun `Check that conversion is not silently failing with identity operation`() {
        assertThrows<Throwable> {
            positionConverter.run {
                assertNear(g2d.run { C2D(lon, lat) }, g2d.toC2D())
                assertNear(c2d.run { G2D(x, y) }, c2d.toG2D())
            }
        }
    }
}