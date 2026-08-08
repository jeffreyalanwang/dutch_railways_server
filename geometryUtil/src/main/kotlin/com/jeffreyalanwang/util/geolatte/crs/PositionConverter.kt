package com.jeffreyalanwang.util.geolatte.crs

import com.jeffreyalanwang.util.geolatte.crs.PositionConverter.Companion.invoke
import org.geolatte.geom.Position
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

/**
 * Convert GeoLatte [org.geolatte.geom.Position]s using intermediate proj4j [org.locationtech.proj4j.CoordinateReferenceSystem]s.
 *
 * Not thread-safe.
 */
internal class PositionConverter<P1: Position, P2: Position>(
    proj4jCrs1: CoordinateReferenceSystem,
    proj4jCrs2: CoordinateReferenceSystem,
    private val constructP1: PositionConstructor<P1>,
    private val constructP2: PositionConstructor<P2>,
) {
    private val transform1to2 = factory.createTransform(proj4jCrs1, proj4jCrs2)
    private val transform2to1 = factory.createTransform(proj4jCrs2, proj4jCrs1)

    fun P1.toP2(): P2 = transform1to2(this, constructP2)
    fun P2.toP1(): P1 = transform2to1(this, constructP1)

    companion object {
        typealias PositionConstructor<P> = (x: Double, y: Double, z: Double) -> P

        private val factory = CoordinateTransformFactory()

        private inline operator fun <P1: Position, P2: Position> CoordinateTransform.invoke(
            p1: P1,
            constructP2: PositionConstructor<P2>,
            tempDest: () -> ProjCoordinate = { ProjCoordinate() },
        ) = p1.toProjCoordinate()
            .let { transform(it, tempDest()) }
            .run { constructP2(x, y, z) }
    }
}

internal fun <P: Position> P.toProjCoordinate() = ProjCoordinate(
    getCoordinate(0),
    getCoordinate(1),
    if (coordinateDimension > 2) getCoordinate(2) else Double.NaN,
)