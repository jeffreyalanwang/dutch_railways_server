package com.jeffreyalanwang.dutchrailways.api.util

import org.geolatte.geom.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <reified P: Position> buildPositionSequence(
    size: Int,
    builder: PositionSequenceBuilder<P>.() -> Unit,
): PositionSequence<P> =
    PositionSequenceBuilders.fixedSized(size, P::class.java)
    .apply(builder)
    .toPositionSequence()

inline fun <reified P: Position> buildPositionSequence(
    builder: PositionSequenceBuilder<P>.() -> Unit,
): PositionSequence<P> =
    PositionSequenceBuilders.variableSized(P::class.java)
    .apply(builder)
    .toPositionSequence()

inline fun <S, reified T: Position> Iterable<S>.mapToPositionSequence(
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

inline fun <reified P: Position> PositionSequence(source: Iterable<P>) = source.mapToPositionSequence { it }

inline fun <S, reified T: LinearRing<P>, P: Position> Iterable<S>.mapToPolygon(transform: (S) -> T) =
    Polygon(*mapToArray(transform))

inline fun <S, reified T: Polygon<P>, P: Position> Iterable<S>.mapToMultiPolygon(transform: (S) -> T) =
    MultiPolygon(*mapToArray(transform))

inline fun <S, reified T> Iterable<S>.mapToArray(transform: (S) -> T) = iterator().run {
    Array(sizeOrElse()) { transform(next()) }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Iterable<T>.sizeOrElse(default: () -> Int = { count() }): Int {
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