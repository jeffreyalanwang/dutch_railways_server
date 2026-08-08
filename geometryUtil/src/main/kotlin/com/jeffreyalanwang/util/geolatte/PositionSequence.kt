package com.jeffreyalanwang.util.geolatte

import org.geolatte.geom.*

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
): PositionSequence<T> = geoAwareSizeOrNull()
    ?.let { size ->
        buildPositionSequence(size) {
            forEach { s: S -> add(transform(s)) }
        }
    }
    ?:run {
        buildPositionSequence {
            forEach { s: S -> add(transform(s)) }
        }
    }

public inline fun <reified P: Position> PositionSequence(source: Iterable<P>): PositionSequence<P> =
    source.mapToPositionSequence { it }
