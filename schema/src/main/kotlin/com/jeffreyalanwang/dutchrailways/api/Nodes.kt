package com.jeffreyalanwang.dutchrailways.api

interface PassService {
    val id: Int
    val name: String
    val trainset: Trainset
    val amenities: List<Amenity>
}

sealed interface Place {
    val id: Int
    val name: String
    val locatedIn: List<Area>
}

interface Area : Place {
    val contains: List<Place>
    val geom: GeoMultiPolygon
}

interface Station : Place {
    val address: String
    val geom: GeoCoords
}