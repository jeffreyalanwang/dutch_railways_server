package com.jeffreyalanwang.dutchrailways.api

import kotlin.reflect.KClass

public data class SearchRequest(
    val anyLike: String?,
    val nameLike: String?,
    val near: GeoRect?,
)

public data class SearchResults(
    val results: List<SearchResultNode>,
    val pageInfo: PageInfo,
)

@ConsistentCopyVisibility
public data class SearchResultNode private constructor(
    val passService: PassService? = null,
    val area: Area? = null,
    val station: Station? = null,
) {
    public constructor(passService: PassService) : this(passService, null, null)
    public constructor(area: Area) : this(null, area, null)
    public constructor(station: Station) : this(null, null, station)

    val type: KClass<out Any> get() = when {
        passService != null -> PassService::class
        area != null -> Area::class
        station != null -> Station::class
        else -> throw IllegalStateException()
    }
}

public data class PageInfo(
    val cursor: String?,
    val hasNextPage: Boolean,
)
