package com.jeffeyalanwang.dutchrailways.backend.server

import graphql.com.google.common.collect.ImmutableList
import java.time.ZonedDateTime

enum class Endpoint { Origin, Destination }
enum class StopPoint { Arrival, Departure }

enum class TrainsetQuality {OLD, NEW}
enum class Trainset(val quality: TrainsetQuality) {
    SLT(TrainsetQuality.OLD),
    ICM(TrainsetQuality.OLD),
    DDZ(TrainsetQuality.OLD),
    VIRM(TrainsetQuality.OLD),
    SNG(TrainsetQuality.NEW),
    ICNG(TrainsetQuality.NEW),
    GTW(TrainsetQuality.NEW),
    Flirt(TrainsetQuality.NEW),
}

enum class TrainAmenity(val friendlyName: String) {
    STROOM("Power outlets"),
    TOILET("Restrooms"),
    WIFI("Wi-Fi"),
    STILTE("Quiet car"),
    FIETS("Bicycle stowage"),
    TOEGANKELIJK("Accessible"),
    unknown("Unknown")
}

data class Journey(
    val stops: ImmutableList<ServiceStop>
)

sealed interface Place {
    val id: Int
    val name: String
}

data class Area(
    override val id: Int,
    override val name: String,
) : Place

data class Station(
    override val id: Int,
    override val name: String,
    val address: String,
) : Place {
    companion object {
        val all = listOf(
            Station(
                358,
                "Amsterdam Centraal",
                "5a, IJ-hal, Centrum, Amsterdam, Noord-Holland, Nederland, 1012 AA, Nederland",
            ),
            Station(
                361,
                "Rotterdam Centraal",
                "Spoor 8, Stationssingel, Provenierswijk, Noord, Rotterdam, Zuid-Holland, Nederland, 3033 HB, Nederland",
            ),
            Station(
                376,
                "Den Haag HS",
                "Stationsplein, Stationsbuurt, Centrum, Den Haag, Zuid-Holland, Nederland, 2515 RT, Nederland",
            ),
        )
        fun byId(id: Int): Station =
            all.find { it.id == id }!!
    }
}

/**
 * At least one of [arrival] or [departure] will always be non-null.
 */
data class ServiceStop(
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
    val passServiceId: Int,
    val stationId: Int,
)

data class PassService(
    val id: Int,
    val title: String,
    val trainset: Trainset,
    val amenities: Set<TrainAmenity>,
)