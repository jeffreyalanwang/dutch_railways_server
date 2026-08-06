package com.jeffreyalanwang.dutchrailways.api.util

import org.geolatte.geom.Position
import org.geolatte.geom.PositionSequence
import org.geolatte.geom.PositionSequenceBuilder
import org.geolatte.geom.PositionSequenceBuilders

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

inline fun <reified P: Position> PositionSequence(
    source: Collection<P>,
) = buildPositionSequence(source.size) { addAll(source) }

inline fun <S : Position, reified T: Position> PositionSequence<S>.mapPositionSequence(
    transform: (S) -> T,
) = buildPositionSequence(size()) {
        forEach { s: S ->
            add(transform(s))
        }
    }

inline fun <S, reified T: Position> Collection<S>.mapPositionSequence(
    transform: (S) -> T,
) = buildPositionSequence(size) {
        forEach { s: S ->
            add(transform(s))
        }
    }