package com.jeffreyalanwang.util.geolatte.crs

import org.geolatte.geom.C2D
import org.geolatte.geom.G2D

data class CrsConversionTestValue<C2D, G2D>(val c2d: C2D, val g2d: G2D) {
    internal companion object {
        fun testPointsOf(vararg testPoints: Pair<C2D, G2D>) =
            testPoints.map { (c2d, g2d) ->
                CrsConversionTestValue(c2d = c2d, g2d = g2d)
            }

        fun <T, R, C2D : T, G2D : T> CrsConversionTestValue<C2D, G2D>.map(block: (T) -> R) =
            CrsConversionTestValue(
                c2d = block(c2d),
                g2d = block(g2d),
            )

        fun <C2D, G2D> Iterable<CrsConversionTestValue<C2D, G2D>>.unzip() =
            CrsConversionTestValue(c2d = map { it.c2d }, g2d = map { it.g2d })

        fun <C2D, G2D> CrsConversionTestValue<Iterable<C2D>, Iterable<G2D>>.zip() =
            c2d.zip(g2d) { c2dItem, g2dItem ->
                CrsConversionTestValue(c2d = c2dItem, g2d = g2dItem)
            }
    }
}