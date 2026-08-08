package com.jeffreyalanwang.dutchrailways.api

public interface PassService {
    public val id: Int
    public val name: String
    public val trainset: Trainset
    public val amenities: List<Amenity>
}

public sealed interface Place {
    public val id: Int
    public val name: String
    public val locatedIn: List<Area>
}

public interface Area : Place {
    public val contains: List<Place>
    public val geom: GeoMultiPolygon
}

public interface Station : Place {
    public val address: String
    public val geom: GeoCoords
}