package com.jeffreyalanwang.dutchrailways.backend.server.processing.search

import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import kotlin.reflect.KClass

class SearchFieldPaths(
    val allStrings: Array<String>,
    val name: String,
    val geom: String? = null,
) {
    /** Map of repository entities to their string-queryable fields. */
    companion object : Map<KClass<out Any>, SearchFieldPaths> by mapOf(
        PassService::class to SearchFieldPaths(
            allStrings = arrayOf("name", "consist.name", "consist.amenities.description"),
            name = "name",
        ),
        Area::class to SearchFieldPaths(
            allStrings = arrayOf("name"),
            name = "name",
            geom = "geom",
        ),
        Station::class to SearchFieldPaths(
            allStrings = arrayOf("name", "address"),
            name = "name",
            geom = "geom",
        ),
    ) {
        val allStrings = values.flatMap { it.allStrings.toList() }.toSet().toTypedArray()
        val name = values.map { it.name }.toSet().toTypedArray()
        val geom = values.map { it.geom }.toSet().toTypedArray()
    }
}

val <T : Any> Collection<KClass<out T>>.arr  get() = toTypedArray()
val <T : Any> Collection< Class<out T>>.arr  get() = toTypedArray()
val <T : Any> Iterable  <KClass<out T>>.java get() = map { it.java }
val <T : Any> Array     <KClass<out T>>.java get() = Array(size) { i -> this[i].java }
