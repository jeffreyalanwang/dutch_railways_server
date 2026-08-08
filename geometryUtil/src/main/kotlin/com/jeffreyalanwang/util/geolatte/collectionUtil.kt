package com.jeffreyalanwang.util.geolatte

import org.geolatte.geom.Complex
import org.geolatte.geom.LineString

@PublishedApi
internal inline fun <S, reified T> Iterable<S>.mapToArray(
    size: Int = geoAwareSizeOrElse { count() },
    transform: (S) -> T,
): Array<T> =
    iterator().run {
        Array(size) { transform(next()) }
            .also { check(!hasNext()) }
    }

public inline fun <T> Iterable<T>.geoAwareSizeOrElse(default: () -> Int): Int =
    when (this) {
        is Collection -> size
        is Complex<*, *> -> numGeometries
        is LineString<*> -> positions.size()
        else -> default()
    }

public fun <T> Iterable<T>.geoAwareSizeOrNull(): Int? = geoAwareSizeOrElse { return null }
