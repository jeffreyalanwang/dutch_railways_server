package com.jeffreyalanwang.dutchrailways.api

data class PassService(
    val id: Int,
    val name: String,
    val trainset: Trainset,
    val amenities: List<Amenity>,
)

interface Place {
    val id: Int
    val name: String
    val locatedIn: List<Area>
}

data class Area(
    override val id: Int,
    override val name: String,
    override val locatedIn: List<Area>,
    val contains: List<Place>,
    val geom: GeoMultiPolygon,
): Place

data class Station(
    override val id: Int,
    override val name: String,
    override val locatedIn: List<Area>,
    val address: String,
    val geom: GeoCoords,
): Place