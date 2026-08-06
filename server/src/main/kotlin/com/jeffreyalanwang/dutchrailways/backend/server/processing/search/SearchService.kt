package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.InitReindexBehavior.ReindexAll
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory
import org.hibernate.search.engine.search.query.SearchFetchable
import org.hibernate.search.engine.spatial.GeoBoundingBox
import org.hibernate.search.engine.spatial.GeoPoint
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
            .search(types.java)
            .where {
                val paths = SearchFieldPaths[types] // Get only the field paths for the types passed into [search] above
                it.bool()
                    .shouldIfNotNull(query = anyLike) { query, f ->
                        f.match().fields(*paths.allStrings.arr()).matching(query).fuzzy(2)
                    }
                    .shouldIfNotNull(query = nameLike) { query, f ->
                        f.match().fields(*paths.name.arr()).matching(query).fuzzy(2)
                    }
                    .shouldIfNotNull(query = near) { query, f ->
                        f.spatial().within().fields(*paths.geom.arr()).boundingBox(query.toHibernateSearch())
                    }
            }
            .flow(batchSize)
            .let { emitAll(it) }
    }

}

private fun <Q, SR> BooleanPredicateClausesStep<SR, *>.shouldIfNotNull(
    query: Q?,
    clauseContributor: (query: Q, f: TypedSearchPredicateFactory<SR>) -> PredicateFinalStep,
) = if (query == null) this else should { f -> clauseContributor(query, f) }

private fun GeoRect.toHibernateSearch() = object : GeoBoundingBox {
    override fun topLeft() = this@toHibernateSearch.northwest.toHibernateSearch()
    override fun bottomRight() = this@toHibernateSearch.southeast.toHibernateSearch()
}

private fun GeoCoords.toHibernateSearch() = object : GeoPoint {
    override fun latitude() = this@toHibernateSearch.latitude
    override fun longitude() = this@toHibernateSearch.longitude
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
