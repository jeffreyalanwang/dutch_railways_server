package com.jeffreyalanwang.dutchrailways.api.util

interface GeoCoords {
    val latitude: Double
    val longitude: Double
}

interface GeoLinearRing {
    val points: List<GeoCoords>
}

interface GeoPolygon {
    val rings: List<GeoLinearRing>
}

interface GeoMultiPolygon {
    val polygons: List<GeoPolygon>
}
