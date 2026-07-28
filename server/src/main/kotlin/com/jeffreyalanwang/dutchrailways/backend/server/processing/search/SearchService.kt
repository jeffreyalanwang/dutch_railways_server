package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.util.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.InitReindexBehavior.ReindexAll
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.hibernate.search.engine.search.query.SearchFetchable
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.massindexing.MassIndexer
import org.hibernate.search.mapper.orm.session.SearchSession
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.lang.Runtime.getRuntime
import kotlin.reflect.KClass

private val THREAD_COUNT = getRuntime().availableProcessors() * 2

@Service
class SearchService(
    reindexOnInit: InitReindexBehavior = ReindexAll,
    transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) {
    private fun newSession(): SearchSession = Search.session(entityManager)

    private fun newIndexer(): MassIndexer = newSession()
        .massIndexer()
        .threadsToLoadObjects(THREAD_COUNT)

    final val initJob: Job = transactionTemplate.execute {
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
