package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.jeffreyalanwang.dutchrailways.api.GeoRect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.data.pagination.CursorEncoder.base64
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.graphql.data.pagination.CursorStrategy.withEncoder
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * We put all args into the cursor so that it can function as an ID,
 * thus implicitly cacheable by GraphQL clients.
 */
data class SearchCursorData(
    val anyLike: String?,
    val nameLike: String?,
    val near: GeoRect?,

    val types: Collection<String>,
    val after: Int,
) {
    fun incrementedBy(count: Int) = copy(after = after + count)
}

@Configuration
class SearchCursorStrategyConfig {
    @Bean
    fun kotlinJacksonModule() = KotlinModule.Builder()
        .enable(KotlinFeature.StrictNullChecks)
        .build()
}

@Component
class SearchCursorStrategy(
    val mapper: ObjectMapper
): CursorStrategy<SearchCursorData> {
    override fun supports(targetType: Class<*>) =
        targetType == SearchCursorData::class.java

    override fun toCursor(position: SearchCursorData): String =
        mapper.writeValueAsString(position)

    override fun fromCursor(cursor: String): SearchCursorData =
        mapper.readValue(cursor, SearchCursorData::class.java)
}

@Component
class EncodingSearchCursorStrategy(val delegate: SearchCursorStrategy):
    CursorStrategy<SearchCursorData> by withEncoder(delegate, base64())
