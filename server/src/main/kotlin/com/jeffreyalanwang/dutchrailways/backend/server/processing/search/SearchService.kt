package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.util.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.InitReindexBehavior.ReindexAll
import jakarta.persistence.EntityManager
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.hibernate.search.engine.search.query.SearchFetchable
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.massindexing.MassIndexer
import org.hibernate.search.mapper.orm.session.SearchSession
import org.springframework.stereotype.Service
import java.lang.Runtime.getRuntime
import kotlin.let
import kotlin.reflect.KClass

private val THREAD_COUNT = getRuntime().availableProcessors() * 2

@Service
class SearchService(
    reindexOnInit: InitReindexBehavior = ReindexAll,
    private val entityManager: EntityManager,
) {
    private fun newSession(): SearchSession = Search.session(entityManager)

    private fun newIndexer(): MassIndexer = newSession()
        .scope(SearchFieldPaths.keys.java)
        .massIndexer()
        .threadsToLoadObjects(THREAD_COUNT)

    val initJob = run {
        reindexOnInit.async { newIndexer() }
    }

    fun <T : Any> search(
        anyLike: String?,
        nameLike: String?,
        near: GeoRect?,
        types: Collection<KClass<out T>>,
        batchSize: Int,
    ) = flow {
        initJob.join()

        newSession()
            .search(SearchFieldPaths.keys.java)
            .where {
                mapOf(
                    anyLike to SearchFieldPaths.allStrings,
                    nameLike to SearchFieldPaths.name,
                ).filterKeys { k ->
                    k != null
                }.toList().fold(it.bool()) { clauses, (queryString, fieldPaths) ->
                    clauses.must { f ->
                        f.match().fields(*fieldPaths).matching(queryString)
                    }
                }
            }
            .flow(batchSize)
            .let { emitAll(it) }
    }
}

private fun <T> SearchFetchable<T>.flow(batchSize: Int) = flow<T> {
    scroll(batchSize).use { resultScroll ->
        while (true) resultScroll.next().run {
            if (!hasHits()) return@flow
            for (hit in hits()) {
                emit(hit)
            }
        }
    }
}
