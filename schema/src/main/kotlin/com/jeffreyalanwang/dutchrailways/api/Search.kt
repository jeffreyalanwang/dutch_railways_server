package com.jeffreyalanwang.dutchrailways.api

data class SearchRequest(
    val anyLike: String?,
    val nameLike: String?,
    val near: GeoRect?,
)

data class SearchResults(
    val results: List<SearchResultNode>,
    val pageInfo: PageInfo,
)

data class SearchResultNode(
    val passService: PassService? = null,
    val area: Area? = null,
    val station: Station? = null,
)

data class PageInfo(
    val cursor: String?,
    val hasNextPage: Boolean,
)
