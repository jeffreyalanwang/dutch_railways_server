package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.DataConverter
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.StationNotFoundException
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.buildGraphStations
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.generateGraphTrips
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.GraphAttribute
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Station
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Trip

private class TransitGraphImpl(
    private val trips: GraphAttribute<TripId, Trip>,
    private val stations: GraphAttribute<StationId, Station>,
): TransitGraph {
    override operator fun get(tripId: TripId) = trips[tripId]
    override operator fun get(stationId: StationId) = stations[stationId]
    override val tripCount get() = trips.size
    override val stationCount get() = stations.size
}

private class DataConverterImpl<ETrip: Any, EStation: Any>(
    private val tripsKey: List<ETrip>,
    private val stationsKey: List<EStation>,
): DataConverter<ETrip, EStation> {
    override fun EStation.convertToInternal() =
        stationsKey.indexOf(this)
        .also {
            if (it < 0) throw StationNotFoundException(this, stationsKey)
        }
        .let { StationId(it) }

    override fun TripId.convertToExternal() = tripsKey[index]
    override fun StationId.convertToExternal() = stationsKey[index]
}

/**
 * Internally, [Trip] and [Station] objects reference each other; in addition,
 * they do so by each others' index in their respective master collection.
 *
 * This class sets up these internal objects and provides a way to convert with
 * the external model.
 */
class RouteQueryDataSource<ETrip: Any, EStation: Any> private constructor(
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
         * Type parameters need to implement [equals] and [hashCode].
         *
         * @param stops Must be co-indexed with [trips].
         *              Flattened stop times must be sorted and unique.
         */
        fun <KTrip: Any, KStation: Any> fromRelational(
            trips: Iterable<KTrip>,
            stations: Iterable<KStation>,
            stops: Iterable<GenericTripDetails<KStation>>,
        ): RouteQueryDataSource<KTrip, KStation> {

            val (stationsInternal, tripsInternal) = buildGraphStations(stations) { stationLookup ->
                stops.generateGraphTrips { tripId, eStation ->
                    stationLookup(tripId, eStation)
                }
            }

            return RouteQueryDataSource(
                trips = GraphAttribute(tripsInternal.toTypedArray()),
                stations = GraphAttribute(stationsInternal.toTypedArray()),
                tripsKey = trips.toList(),
                stationsKey = stations.toList(),
            )

        }

        /**
         * @see [fromRelational].
         */
        fun <KTrip: Any, KStation: Any> fromRelational(
            trips: Iterable<Pair<KTrip, GenericTripDetails<KStation>>>,
            stations: Iterable<KStation>,
        ) = with(trips.unzip()) { fromRelational(first, stations, second) }

        /**
         *
         * Type parameters need to implement [equals] and [hashCode].
         *
         * @param stops  Must be co-indexed with [trips].
         *               Flattened stop times must be sorted and unique.
         */
        fun <ETrip: Any, EStation: Any> fromTrips(
            trips: Iterable<ETrip>,
            stops: Iterable<GenericTripDetails<EStation>>,
        ): RouteQueryDataSource<ETrip, EStation> {

            val (stationsInternal, stationsExternal, tripsInternal) = buildGraphStations { stationLookup ->
                stops.generateGraphTrips { tripId, eStation ->
                    stationLookup(tripId, eStation)
                }
            }

            return RouteQueryDataSource(
                trips = GraphAttribute(tripsInternal.toTypedArray()),
                stations = GraphAttribute(stationsInternal.toTypedArray()),
                tripsKey = trips.toList(),
                stationsKey = stationsExternal,
            )
        }

        fun <ETrip: Any, EStation: Any> fromTrips(
            vararg trips: Pair<ETrip, GenericTripDetails<EStation>>,
        ) = with(trips.unzip()) { fromTrips(first, second) }
    }
}

