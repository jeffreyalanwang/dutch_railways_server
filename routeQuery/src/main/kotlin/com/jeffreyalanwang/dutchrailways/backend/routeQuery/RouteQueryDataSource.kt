package com.jeffreyalanwang.dutchrailways.backend.routeQuery.model

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.GraphAttribute
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip
import java.util.*

private fun GenericTripDetails.Leg.asTripLeg() =
    Trip.Leg(
        departTime = departTime,
        arrivalTime = arrivalTime,
    )

private class TransitGraphImpl(
    private val trips: GraphAttribute<TripId, Trip>,
    private val stations: GraphAttribute<StationId, Station>,
): TransitGraph {
    override operator fun get(tripId: TripId) = trips[tripId]
    override operator fun get(stationId: StationId) = stations[stationId]
    override val tripCount get() = trips.size
    override val stationCount get() = stations.size
}

private class DataConverterImpl<ETrip, EStation>(
    private val tripsKey: List<ETrip>,
    private val stationsKey: List<EStation>,
): DataConverter<ETrip, EStation> {
    override fun EStation.convertToInternal() = stationsKey.indexOf(this).also { require(it >= 0) }.let { StationId(it) }

    override fun TripId.convertToExternal() = tripsKey[index]
    override fun StationId.convertToExternal() = stationsKey[index]
}

/**
 * Internally, [com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip]
 * and [com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station]
 * objects reference each other; in addition, they do so by each others' index in
 * their respective master collection.
 *
 * This class sets up these internal objects and provides a way to convert with the external model.
 */
class RouteQueryDataSource<ETrip, EStation> private constructor(
    trips: GraphAttribute<TripId, Trip>,
    stations: GraphAttribute<StationId, Station>,
    tripsKey: List<ETrip>,
    stationsKey: List<EStation>,
) {
    internal val transitGraph: TransitGraph = TransitGraphImpl(trips, stations)
    internal val dataConverter: DataConverter<ETrip, EStation> = DataConverterImpl(tripsKey, stationsKey)

    companion object {
        /**
         * Create a [RouteQueryDataSource] from a relational model,
         * where we can cheaply create a set of [Station]s instead of
         * generating [stationsKey] by hand.
         *
         * We use [LinkedHashMap] to preserve insertion order.
         *
         * Type parameters need to implement [equals] and [hashCode].
         *
         * @param stops  Must be co-indexed with [trips].
         */
        fun <KTrip, KStation> fromRelational(
            trips: Iterable<KTrip>,
            stations: Iterable<KStation>,
            stops: Iterable<GenericTripDetails<KStation>>,
        ): RouteQueryDataSource<KTrip, KStation> {

            // Allow quick lookup, given a [KStation],
            // its ID and the running list of visiting trips.
            val stationMap = stations.withIndex().associate { (i, it) ->
                it to Pair(
                    i,
                    mutableListOf<TripId>(),
                )
            }

            val tripsInternal = stops
                .mapIndexed { tripIndex, tripDetails ->
                    val (stationMasterIndexes, stationTripIdLists) = tripDetails.stations.map { stationMap[it]!! }.unzip()

                    // Side effect: add to the station's list.
                    stationTripIdLists.forEach { it += TripId(tripIndex) }

                    Trip(
                        stations = stationMasterIndexes.map { StationId(it) },
                        legs = tripDetails.times.map { it.asTripLeg() },
                    )
                }

            // This operation only works because maps preserve insertion order.
            val stationsInternal =
                stationMap.values.map { (_, tripIds) -> Station(tripIds) }

            return RouteQueryDataSource(
                trips = GraphAttribute(tripsInternal.toTypedArray()),
                stations = GraphAttribute(stationsInternal.toTypedArray()),
                tripsKey = trips.toList(),
                stationsKey = stations.toList(),
            )

        }

        /**
         * @param stops  Must be co-indexed with [trips].
         */
        fun <ETrip, EStation> fromTrips(
            trips: Iterable<ETrip>,
            stops: Iterable<GenericTripDetails<EStation>>,
        ): RouteQueryDataSource<ETrip, EStation> {

            // LinkedHashMap preserves insertion order.
            val stationData = LinkedHashMap<EStation, LinkedList<TripId>>()

            val tripsInternal = stops.mapIndexed { tripIndex, tripDetails ->
                val stationIds = tripDetails.stations
                    .map { externalStation ->
                        var index = stationData.keys.indexOf(externalStation)
                        if (index < 0) {
                            // Side effect: add new station list to the map if not found.
                            index = stationData.size
                            stationData[externalStation] =
                                LinkedList<TripId>()
                        }
                        // Side effect: add to the station's list.
                        stationData[externalStation]!! += TripId(tripIndex)
                        StationId(index)
                    }

                Trip(
                    stations = stationIds,
                    legs = tripDetails.times.map { it.asTripLeg() },
                )
            }

            val stationsExternal = stationData.keys.toList()
            val stationsInternal = stationData.map { (_, tripIds) ->
                Station(
                    tripIds
                )
            }

            return RouteQueryDataSource(
                trips = GraphAttribute(tripsInternal.toTypedArray()),
                stations = GraphAttribute(stationsInternal.toTypedArray()),
                tripsKey = trips.toList(),
                stationsKey = stationsExternal.toList(),
            )
        }

        fun <ETrip, EStation> fromTrips(
            vararg trips: Pair<ETrip, GenericTripDetails<EStation>>,
        ) = with(trips.unzip()) { fromTrips(first, second) }
    }
}
