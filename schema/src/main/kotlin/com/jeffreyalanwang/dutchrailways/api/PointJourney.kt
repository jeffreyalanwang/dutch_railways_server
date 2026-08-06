package com.jeffreyalanwang.dutchrailways.api

import java.time.OffsetDateTime

/**
 * A journey represented as a list of time/place points.
 */
open class PointJourney(val points: List<JourneyPoint>) {

    open class JourneyPoint(
        val time: OffsetDateTime,
        val station: Int,
        val passService: Int?,
    )

    companion object {
        fun ofSingleStop(
            time: OffsetDateTime,
            place: Int,
        ) = PointJourney(
                points = listOf(
                    JourneyPoint(
                        time = time,
                        station = place,
                        passService = null,
                    ),
                )
            )

        infix fun departingAt(time: OffsetDateTime) = object : Builder.ReadyForDepartureStation {
            override val completedPortion get() = emptyList<JourneyPoint>()
            override val departureTime get() = time
        }

        infix fun PointJourney.departingAt(time: OffsetDateTime) = object : Builder.ReadyForDepartureStationOrPassService {
            override val completedPortion get() = this@departingAt.points
            override val departureTime get() = time
            override val departureStation get() = this@departingAt.points.last().station
        }

    }

    interface Builder {
        val completedPortion: List<JourneyPoint>

        interface ReadyForDepartureStation: Builder {
            val departureTime: OffsetDateTime
            infix fun fromStation(id: Int) = object : ReadyForPassService {
                override val completedPortion get() = this@ReadyForDepartureStation.completedPortion
                override val departureTime get() = this@ReadyForDepartureStation.departureTime
                override val departureStation get() = id
            }
        }

        interface ReadyForPassService: Builder {
            val departureTime: OffsetDateTime
            val departureStation: Int
            infix fun viaPassService(id: Int) = object : ReadyForArrivalTime {
                override val completedPortion get() = this@ReadyForPassService.completedPortion
                override val departureTime get() = this@ReadyForPassService.departureTime
                override val departureStation get() = this@ReadyForPassService.departureStation
                override val passService get() = id
            }
        }

        interface ReadyForDepartureStationOrPassService: ReadyForDepartureStation, ReadyForPassService
        interface ReadyForArrivalTime: Builder {
            val departureTime: OffsetDateTime
            val departureStation: Int
            val passService: Int
            infix fun arrivingAt(time: OffsetDateTime) = object : ReadyForArrivalStation {
                override val completedPortion get() = this@ReadyForArrivalTime.completedPortion
                override val departureTime get() = this@ReadyForArrivalTime.departureTime
                override val departureStation get() = this@ReadyForArrivalTime.departureStation
                override val passService get() = this@ReadyForArrivalTime.passService
                override val arrivalTime get() = time
            }
        }
        interface ReadyForArrivalStation: Builder {
            val departureTime: OffsetDateTime
            val departureStation: Int
            val passService: Int
            val arrivalTime: OffsetDateTime
            infix fun atStation(id: Int) = PointJourney(
                    completedPortion +
                    JourneyPoint(
                        departureTime,
                        station = departureStation,
                        passService = passService
                    ) +
                    JourneyPoint(
                        arrivalTime,
                        station = id,
                        passService = passService
                    )
                )
        }
    }
}

