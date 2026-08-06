package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.hibernate.search.engine.search.query.SearchFetchable

internal fun <T> SearchFetchable<T>.flow(batchSize: Int) = flow<T> {
    scroll(batchSize).use { resultScroll ->
        while (true) resultScroll.next().run {
            if (!hasHits()) return@flow
            for (hit in hits()) {
                emit(hit)
            }
        }
    }
}

/** Terminal operation. */
suspend inline fun <T> Flow<T>.takeAndHasNext(count: Int) = takeAndHasNext(count) { it }

/** Terminal operation. */
suspend inline fun <T, R> Flow<T>.takeAndHasNext(
    count: Int,
    crossinline transform: (T) -> R,
): Pair<List<R>, Boolean> {
    val taken = ArrayList<R>(count)
    var hasNext = false
    take(count + 1).collectIndexed { i, item ->
        if (i < count) {
            taken += transform(item)
        } else {
            hasNext = true
        }
    }
    return taken to hasNext
}