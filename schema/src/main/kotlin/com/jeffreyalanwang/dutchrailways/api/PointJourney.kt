package com.jeffreyalanwang.dutchrailways.api

import org.jetbrains.annotations.TestOnly
import java.time.OffsetDateTime

/**
 * A journey represented as a list of time/place points.
 */
public open class PointJourney(public val points: List<JourneyPoint>) {

    public open class JourneyPoint(
        public val time: OffsetDateTime,
        public val station: Int,
        public val passService: Int?,
    )

    public companion object {
        public fun ofSingleStop(
            time: OffsetDateTime,
            place: Int,
        ): PointJourney = PointJourney(
                points = listOf(
                    JourneyPoint(
                        time = time,
                        station = place,
                        passService = null,
                    ),
                )
            )

        @TestOnly
        public infix fun departingAt(time: OffsetDateTime): Builder.ReadyForDepartureStation = object : Builder.ReadyForDepartureStation {
            override val completedPortion get() = emptyList<JourneyPoint>()
            override val departureTime get() = time
        }

        @TestOnly
        public infix fun PointJourney.departingAt(time: OffsetDateTime): Builder.ReadyForDepartureStationOrPassService = object : Builder.ReadyForDepartureStationOrPassService {
            override val completedPortion get() = this@departingAt.points
            override val departureTime get() = time
            override val departureStation get() = this@departingAt.points.last().station
        }

    }
    
    public interface Builder {
        public val completedPortion: List<JourneyPoint>

        public interface ReadyForDepartureStation: Builder {
            public val departureTime: OffsetDateTime
            public infix fun fromStation(id: Int): ReadyForPassService = object : ReadyForPassService {
                override val completedPortion get() = this@ReadyForDepartureStation.completedPortion
                override val departureTime get() = this@ReadyForDepartureStation.departureTime
                override val departureStation get() = id
            }
        }

        public interface ReadyForPassService: Builder {
            public val departureTime: OffsetDateTime
            public val departureStation: Int
            public infix fun viaPassService(id: Int): ReadyForArrivalTime = object : ReadyForArrivalTime {
                override val completedPortion get() = this@ReadyForPassService.completedPortion
                override val departureTime get() = this@ReadyForPassService.departureTime
                override val departureStation get() = this@ReadyForPassService.departureStation
                override val passService get() = id
            }
        }

        public interface ReadyForDepartureStationOrPassService: ReadyForDepartureStation, ReadyForPassService
        public interface ReadyForArrivalTime: Builder {
            public val departureTime: OffsetDateTime
            public val departureStation: Int
            public val passService: Int
            public infix fun arrivingAt(time: OffsetDateTime): ReadyForArrivalStation = object : ReadyForArrivalStation {
                override val completedPortion get() = this@ReadyForArrivalTime.completedPortion
                override val departureTime get() = this@ReadyForArrivalTime.departureTime
                override val departureStation get() = this@ReadyForArrivalTime.departureStation
                override val passService get() = this@ReadyForArrivalTime.passService
                override val arrivalTime get() = time
            }
        }
        public interface ReadyForArrivalStation: Builder {
            public val departureTime: OffsetDateTime
            public val departureStation: Int
            public val passService: Int
            public val arrivalTime: OffsetDateTime
            public infix fun atStation(id: Int): PointJourney = PointJourney(
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

