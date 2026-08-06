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

@ConsistentCopyVisibility
data class SearchResultNode private constructor(
    val passService: PassService? = null,
    val area: Area? = null,
    val station: Station? = null,
) {
    constructor(passService: PassService) : this(passService, null, null)
    constructor(area: Area) : this(null, area, null)
    constructor(station: Station) : this(null, null, station)

    val type get() = when {
        passService != null -> PassService::class
        area != null -> Area::class
        station != null -> Station::class
        else -> throw IllegalStateException()
    }
}

data class PageInfo(
    val cursor: String?,
    val hasNextPage: Boolean,
)
