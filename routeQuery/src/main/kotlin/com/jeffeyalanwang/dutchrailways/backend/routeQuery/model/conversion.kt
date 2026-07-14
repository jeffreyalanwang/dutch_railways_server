package com.jeffeyalanwang.dutchrailways.backend.routeQuery.model

import com.jeffeyalanwang.dutchrailways.backend.routeQuery.indexOfOrPut
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericJourneyDetails
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal.Journey
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal.Station
import com.jeffeyalanwang.dutchrailways.backend.routeQuery.model.internal.Trip

/**
 * Convert an external model of trips + stations into an internal model.
 *
 * Type parameters need to implement [equals] and [hashCode].
 *
 * @param stopDetails   For each trip in the system, a [com.jeffeyalanwang.dutchrailways.backend.routeQuery.GenericTripDetails].
 *
 * @param EStation  The external type of the station identifier.
 * @param ETrip     The external type of the trip identifier.
 *
 * @return  * An array of [Trip]s, whose stations are indices in the below.
 *          * An array of [Station]s, whose trips are indices in the above.
 *          * A map from indices the [Array<Trip>] to the corresponding [ETrip]s.
 *          * A map from indices in [Array<Station>] to the corresponding [EStation]s.
 *
 * @see com.jeffeyalanwang.dutchrailways.backend.routeQuery.GenericTripDetails
 * @see InternalRepresentationResult
 */
internal fun <ETrip, EStation> toInternalRepresentation(
    stopDetails: Map<ETrip, GenericTripDetails<EStation>>
): InternalRepresentationResult<ETrip, EStation> {

    // We take advantage of the fact that [Map]s preserve insertion order.

    /** List of trips in the same order as keys of [stopDetails]. */
    val trips: List<Trip>

    /**
     * Ordered map of external station identifiers to the indexes of their
     * corresponding [Trip]s.
     *
     * Each key-value pair contains enough information to build a [Station].
     */
    val externalStations: Map<EStation, List<Int>>

    externalStations = buildMap<EStation, MutableList<Int>> {
        trips = stopDetails.asIterable().mapIndexed { tripIndex, (externalTrip, tripDetails) ->

            /**
             * Each of the trip's stations, in order,
             * translated to their indices in [externalStations].
             */
            val stationIndexes = tripDetails.stations.map { externalStation ->
                this@buildMap.indexOfOrPut(externalStation) { mutableListOf() }
                    .also { this@buildMap[externalStation]!!.add(tripIndex) }
            }

            return@mapIndexed Trip(
                stations = stationIndexes,
                legs = tripDetails.times,
            )
        }
    }

    return InternalRepresentationResult(
        internalTrips =
            trips
            .toTypedArray(),
        internalStations =
            externalStations
            .values
            .map { tripIndexes -> Station(trips = tripIndexes) }
            .toTypedArray(),
        tripKey =
            stopDetails.keys.toList(),
        stationKey =
            externalStations.keys.toList(),
    )
}

/**
 * @property internalTrips      An array of [Trip]s.
 * @property internalStations   An array of [Station]s.
 * @property tripKey            A list of [ETrip]s co-indexed with [internalTrips].
 * @property stationKey         A list of [EStation]s co-indexed with [internalStations].
 */
internal class InternalRepresentationResult<ETrip, EStation>(
    val internalTrips: Array<Trip>,
    val internalStations: Array<Station>,
    val tripKey: List<ETrip>,
    val stationKey: List<EStation>,
) {
    operator fun component1() = internalTrips
    operator fun component2() = internalStations
    operator fun component3() = tripKey
    operator fun component4() = stationKey
}

/**
 * Convert the output journey from internal model to external model,
 * using saved information from initial conversion of external data.
 */
internal fun <EStation, ETrip> Journey.toGenericModel(
    stationsKey: List<EStation>,
    tripsKey: List<ETrip>
) = GenericJourneyDetails(
    legs.map { leg ->
        val originStation = stationsKey[leg.originStation]
        val trip = tripsKey[leg.trip]

        originStation to trip
    },
    stationsKey[finalStation],
)