package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import kotlin.reflect.KClass

class SearchFieldPaths(
    val allStrings: Set<String>,
    val name: Set<String>,
    val geom: Set<String>? = null,
) {
    private operator fun plus(other: SearchFieldPaths) = SearchFieldPaths(
        allStrings = this.allStrings + other.allStrings,
        name = this.name + other.name,
        geom = (this.geom ?: emptySet()) + (other.geom ?: emptySet()),
    )

    /** Map of repository entities to their string-queryable fields. */
    companion object : Map<KClass<out Any>, SearchFieldPaths> by mapOf(
        PassService::class to SearchFieldPaths(
            allStrings = setOf("name", "consist.name", "consist.amenities.description"),
            name = setOf("name"),
        ),
        Area::class to SearchFieldPaths(
            allStrings = setOf("name"),
            name = setOf("name"),
            geom = setOf("geom"),
        ),
        Station::class to SearchFieldPaths(
            allStrings = setOf("name", "address"),
            name = setOf("name"),
            geom = setOf("geom"),
        ),
    ) {
        operator fun get(keys: Iterable<KClass<out Any>>) = keys.map {
                this[it] ?: throw IllegalArgumentException()
            }.reduce { a, b -> a + b }

        val all = this[keys]
    }
}

inline fun <reified T> Collection<T>.arr() = toTypedArray()
val <T : Any> Iterable <KClass<out T>>.java get() = map { it.java }
val <T : Any> Array    <KClass<out T>>.java get() = Array(size) { i -> this[i].java }
