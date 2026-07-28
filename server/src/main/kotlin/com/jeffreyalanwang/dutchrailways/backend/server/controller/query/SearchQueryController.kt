package com.jeffreyalanwang.dutchrailways.backend.server.controller.query

import com.jeffreyalanwang.dutchrailways.api.PageInfo
import com.jeffreyalanwang.dutchrailways.api.SearchRequest
import com.jeffreyalanwang.dutchrailways.backend.server.dto.SearchResultNode
import com.jeffreyalanwang.dutchrailways.backend.server.dto.SearchResults
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchCursorData
import com.jeffreyalanwang.dutchrailways.backend.server.processing.search.SearchService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import graphql.schema.DataFetchingEnvironment
import graphql.schema.SelectedField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.stereotype.Controller

private const val DEFAULT_SEARCH_RESULT_COUNT = 5

private val searchableEntities = mapOf(
    "passService" to PassService::class,
    "area" to Area::class,
    "station" to Station::class,
)

private fun searchResultNodeOf(it: Any): SearchResultNode = when (it) {
    is PassService -> SearchResultNode(passService = it)
    is Area -> SearchResultNode(area = it)
    is Station -> SearchResultNode(station = it)
    else -> throw IllegalStateException(
        "Search service returned an unrecognized object of type ${it::class}."
    )
}

@Controller
class SearchQueryController(
    private val searchService: SearchService,

    @param:Qualifier("encodingSearchCursorStrategy")
    private val cursorStrategy: CursorStrategy<SearchCursorData>,
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
        val requestedTypes = env.getSelectedFieldsOnChild("results") ?: return BLANK_RESULTS

        return search(
            first = first,

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
        first = next ?: DEFAULT_SEARCH_RESULT_COUNT,
        cursor = fromCursorString(cursor),
    )

    private suspend fun search(
        first: Int,
        cursor: SearchCursorData,
    ): SearchResults {
        val resultsFlow = searchService.search(
            anyLike = cursor.anyLike,
            nameLike = cursor.nameLike,
            near = cursor.near,
            types = cursor.types.map {
                searchableEntities[it] ?: throw IllegalArgumentException()
            },
            batchSize = first,
        )

        val (resultList, hasNext) = resultsFlow
            .drop(cursor.after + 1) // to get start (index 0), after = -1
            .takeAndHasNext(first) { searchResultNodeOf(it) }

        return SearchResults(
            results = resultList,
            pageInfo = PageInfo(
                cursor = if (!hasNext) null else cursor.incrementedBy(first).toCursorString(),
                hasNextPage = hasNext,
            ),
        )
    }
}

private val BLANK_RESULTS = SearchResults(
    results = emptyList(),
    pageInfo = PageInfo(
        cursor = null,
        hasNextPage = false,
    ),
)

/**
 * Get a child field from the one being currently processed, by name.
 *
 * @param name  The actual name, rather than potentially an alias,
 *              of an immediate child field that the client selected.
 */
private fun DataFetchingEnvironment.childField(name: String) = selectionSet.immediateFields.find { it.name == name }

/**
 * Get the set of selected field names which are immediate children of this one.
 */
private val SelectedField.selectedFields get() = selectionSet.immediateFields.map { it.name as String }.toSet()

private fun DataFetchingEnvironment.getSelectedFieldsOnChild(fieldName: String) =
    childField(fieldName)?.selectedFields.takeUnless { it.isNullOrEmpty() }

/** Terminal operation. */
private suspend fun <T, R> Flow<T>.takeAndHasNext(count: Int, transform: (T) -> R): Pair<List<R>, Boolean> {
    val taken = ArrayList<R>(count)
    var hasNext = false
    take(count + 1).collectIndexed { i, item ->
        if (i == 0) {
            taken += transform(item)
        } else {
            hasNext = true
        }
    }
    return taken to hasNext
}