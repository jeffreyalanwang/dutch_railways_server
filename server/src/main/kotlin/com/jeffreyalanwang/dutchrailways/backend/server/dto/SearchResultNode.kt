package com.jeffreyalanwang.dutchrailways.backend.server.dto

import com.jeffreyalanwang.dutchrailways.api.PageInfo
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station

/**
 * Partial duplication of [schema] package so that we
 * can use repository entities in [SearchResultNode].
 */
data class SearchResults(
    val results: List<SearchResultNode>,
    val pageInfo: PageInfo,
)

data class SearchResultNode(
    val passService: PassService? = null,
    val area: Area? = null,
    val station: Station? = null,
)
