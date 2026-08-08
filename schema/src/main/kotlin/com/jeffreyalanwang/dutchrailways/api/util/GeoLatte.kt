package com.jeffreyalanwang.dutchrailways.api.util

import org.geolatte.geom.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public inline fun <reified P: Position> buildPositionSequence(
    size: Int,
    builder: PositionSequenceBuilder<P>.() -> Unit,
): PositionSequence<P> =
    PositionSequenceBuilders.fixedSized(size, P::class.java)
    .apply(builder)
    .toPositionSequence()

public inline fun <reified P: Position> buildPositionSequence(
    builder: PositionSequenceBuilder<P>.() -> Unit,
): PositionSequence<P> =
    PositionSequenceBuilders.variableSized(P::class.java)
    .apply(builder)
    .toPositionSequence()

public inline fun <S, reified T: Position> Iterable<S>.mapToPositionSequence(
    transform: (S) -> T,
): PositionSequence<T> = sizeOrElse {
    return@mapToPositionSequence buildPositionSequence {
        forEach { s: S -> add(transform(s)) }
    }
}.let { size ->
    return@mapToPositionSequence buildPositionSequence(size) {
        forEach { s: S -> add(transform(s)) }
    }
}

public inline fun <reified P: Position> PositionSequence(source: Iterable<P>): PositionSequence<P> = source.mapToPositionSequence { it }

public inline fun <S, reified T: LinearRing<P>, P: Position> Iterable<S>.mapToPolygon(transform: (S) -> T): Polygon<P> =
    Polygon(*mapToArray(transform))

public inline fun <S, reified T: Polygon<P>, P: Position> Iterable<S>.mapToMultiPolygon(transform: (S) -> T): MultiPolygon<P> =
    MultiPolygon(*mapToArray(transform))

public inline fun <S, reified T> Iterable<S>.mapToArray(transform: (S) -> T): Array<T> = iterator().run {
    Array(sizeOrElse()) { transform(next()) }
}

@OptIn(ExperimentalContracts::class)
public inline fun <T> Iterable<T>.sizeOrElse(default: () -> Int = { count() }): Int {
    contract {
        callsInPlace(default, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Collection -> size
        is Complex<*, *> -> numGeometries
        is LineString<*> -> positions.size()
        else -> default()
    }
}