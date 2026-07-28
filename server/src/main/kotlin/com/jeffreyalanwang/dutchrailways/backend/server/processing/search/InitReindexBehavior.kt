package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import org.hibernate.search.mapper.orm.massindexing.MassIndexer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

sealed class InitReindexBehavior {
    protected abstract fun reindex(indexer: Supplier<MassIndexer>): CompletionStage<*>
    fun async(indexer: Supplier<MassIndexer>): Job = reindex(indexer).asDeferred()

    object None: InitReindexBehavior() {
        override fun reindex(indexer: Supplier<MassIndexer>) =
            CompletableFuture.completedFuture(Unit)
    }

    object ReindexAll: InitReindexBehavior() {
        override fun reindex(indexer: Supplier<MassIndexer>) =
            indexer.get()
                .start()
    }

    /** For testing. */
    class IndexFirstN(val limit: Long): InitReindexBehavior() {
        override fun reindex(indexer: Supplier<MassIndexer>) =
            indexer.get()
                .limitIndexedObjectsTo(limit)
                .start()
    }
}