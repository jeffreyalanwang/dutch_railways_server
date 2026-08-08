package com.jeffreyalanwang.util.geolatte

import org.geolatte.geom.LinearRing
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.Polygon
import org.geolatte.geom.Position
import org.geolatte.geom.crs.CoordinateReferenceSystem

public inline fun <
    S,
    reified P: Position,
> Iterable<S>.mapToClosedLinearRing(
    crs: CoordinateReferenceSystem<P>,
    transform: (S) -> P,
): LinearRing<P> = geoAwareSizeOrNull()
    .let { size ->
        size?.let {
            mapTo(ArrayList(size), transform)
        }
        ?: run {
            map(transform)
        }
    }
    .run {
        if (first() == last()) PositionSequence(this)
        else buildPositionSequence(size + 1) {
            addAll(this@run)
            add(this@run.first())
        }
    }
    .let {
        LinearRing(it, crs)
    }

public inline fun <
    S,
    reified T: LinearRing<P>,
    P: Position,
> Iterable<S>.mapToPolygon(transform: (S) -> T): Polygon<P> =
    Polygon(*mapToArray { transform(it) })

public inline fun <
    S,
    reified T: Polygon<P>,
    P: Position,
> Iterable<S>.mapToMultiPolygon(transform: (S) -> T): MultiPolygon<P> =
    MultiPolygon(*mapToArray { transform(it) })