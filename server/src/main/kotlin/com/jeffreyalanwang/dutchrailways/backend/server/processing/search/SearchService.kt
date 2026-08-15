package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.api.GeoCoords
import com.jeffreyalanwang.dutchrailways.api.GeoRect
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.InitReindexBehavior.ReindexAll
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

    val initJob: Job = transactionTemplate.execute {
        reindexOnInit.async { newIndexer() }
    }

    fun <T : Any> search(
        anyLike: String?,
        nameLike: String?,
        near: GeoRect?,
        types: Collection<KClass<out T>>,
        batchSize: Int,
    ) = flow {
        val near = near?.toHibernateSearch()

        initJob.join()

        newSession()
            .search(types.java)
            .where { it ->
                it.bool().apply {
                    val paths = SearchFieldPaths[types] // Get only the field paths for the types passed into [search] above

                    if (anyLike != null) should { it ->
                        it.match()
                            .fields(*paths.allStrings.arr<String>())
                                .matching(anyLike)
                            .fuzzy(2)
                            .boost(.5f)
                    }
                    if (nameLike != null) should { it ->
                        it.match()
                            .fields(*paths.name.arr<String>())
                                .matching(nameLike)
                            .fuzzy(2)
                            .boost(1f)
                    }
                    if (near != null) should { it ->
                        it.spatial().within()
                            .fields(*paths.geom.arr<String>())
                                .boundingBox(near)
                            .boost(2f)
                    }
                }
            }
            .flow(batchSize)
            .let { emitAll(it) }
    }

}

private fun GeoRect.toHibernateSearch() = object : GeoBoundingBox {
    override fun topLeft() = this@toHibernateSearch.northwest.toHibernateSearch()
    override fun bottomRight() = this@toHibernateSearch.southeast.toHibernateSearch()
}

private fun GeoCoords.toHibernateSearch() = object : GeoPoint {
    override fun latitude() = this@toHibernateSearch.latitude
    override fun longitude() = this@toHibernateSearch.longitude
}
