package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.PageInfo
import com.jeffreyalanwang.dutchrailways.api.SearchRequest
import com.jeffreyalanwang.dutchrailways.api.SearchResultNode
import com.jeffreyalanwang.dutchrailways.backend.server.controller.childField
import com.jeffreyalanwang.dutchrailways.backend.server.controller.selectedFields
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchCursorData
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchService
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.takeAndHasNext
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.flow.drop
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.stereotype.Controller

/**
 * Keys must match the field names in [SearchResultNode].
 *
 * Values must be [kotlin.reflect.KClass]es of the types
 * used by [SearchService], i.e. Hibernate entity classes.
 */
private val searchableEntities = mapOf(
    "passService" to PassService::class,
    "area" to Area::class,
    "station" to Station::class,
)

private fun Any.toSearchResultNode() = searchableEntities
    .asIterable()
    .associate { (fieldName, entityClass) ->
        if (entityClass.isInstance(this)) {
            fieldName to this
        } else {
            fieldName to null
        }
    }

@Controller
class SearchQueryController(
    private val searchService: SearchService,

    @param:Qualifier("encodingSearchCursorStrategy")
    private val cursorStrategy: CursorStrategy<SearchCursorData>,

    private val DEFAULT_SEARCH_RESULT_COUNT: Int = 5,
) {
    private fun SearchCursorData.toCursorString() = cursorStrategy.toCursor(this)
    private fun fromCursorString(string: String) = cursorStrategy.fromCursor(string)

    @QueryMapping
    suspend fun searchQuery(
        @Argument first: Int?,
        @Argument request: SearchRequest,
        env: DataFetchingEnvironment,
    ): SearchResults {
        val first = first ?: DEFAULT_SEARCH_RESULT_COUNT
        val requestedTypes = env.childField("results")?.selectedFields
        if (requestedTypes.isNullOrEmpty()) return SearchResults.BLANK

        return search(
            count = first,

            cursor = SearchCursorData(
                anyLike = request.anyLike,
                nameLike = request.nameLike,
                near = request.near,
                types = requestedTypes,

                after = -1,
            ),
        )
    }

    @QueryMapping
    suspend fun searchLoadMore(@Argument next: Int?, @Argument cursor: String) = search(
        count = next ?: DEFAULT_SEARCH_RESULT_COUNT,
        cursor = fromCursorString(cursor),
    )

    private suspend fun search(
        count: Int,
        cursor: SearchCursorData,
    ): SearchResults {
        val resultsFlow = searchService.search(
            anyLike = cursor.anyLike,
            nameLike = cursor.nameLike,
            near = cursor.near,
            types = cursor.types.map {
                searchableEntities[it] ?: throw IllegalArgumentException()
            },
            batchSize = count,
        )

        val (resultList, hasNext) = resultsFlow
            .drop(cursor.after + 1) // to get start (index 0), after = -1
            .takeAndHasNext(count)

        return SearchResults(
            resultList,
            hasNextPage = hasNext,
            cursor = if (!hasNext) null else cursor.incrementedBy(count),
        )
    }

    @SchemaMapping
    fun SearchResults.results() = results.map { it.toSearchResultNode() }

    @SchemaMapping
    fun SearchResults.pageInfo() = results.run {
        PageInfo(
            cursor = cursor?.toCursorString(),
            hasNextPage = hasNextPage
        )
    }

    data class SearchResults(
        val results: List<Any>,
        val cursor: SearchCursorData?,
        val hasNextPage: Boolean,
    ) {
        companion object {
            val BLANK = SearchResults(
                results = emptyList(),
                cursor = null,
                hasNextPage = false,
            )
        }
    }
}
