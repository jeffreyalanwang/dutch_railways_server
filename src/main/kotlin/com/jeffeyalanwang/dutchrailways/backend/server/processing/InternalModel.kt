package com.jeffeyalanwang.dutchrailways.backend.server.processing
import kotlin.collections.map
import kotlin.time.Instant

/**
 * Convert an external model of trips + stations into an internal model.
 *
 * Type parameters need to implement [equals] and [hashCode].
 *
 * @param stopDetails   For each trip in the system, a [GenericTripDetails].
 *
 * @param EStation  The external type of the station identifier.
 * @param ETrip     The external type of the trip identifier.
 *
 * @return  * An array of [Trip]s, whose stations are indices in the below.
 *          * An array of [Station]s, whose trips are indices in the above.
 *          * A map from indices the [Array<Trip>] to the corresponding [ETrip]s.
 *          * A map from indices in [Array<Station>] to the corresponding [EStation]s.
 *
 * @see GenericTripDetails
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

/**
 * @property stations       Index of station in their master list.
 * @property legs           `legs[i]` stores the time of depature from
 *                          `stations[i]` and the time of arrival to
 *                          stations[i + 1].
 */
internal data class Trip(
    val stations: List<Int>,
    val legs: List<Pair<Instant, Instant>>,
) {
    /**
     * @return  Time that this trip departs [station],
     *          or `null` if:
     *          * It does not visit [station]
     *          * It terminates at [station]
     */
    fun departTimeAtStation(station: Int) =
        stations.indexOf(station)
            .takeUnless { it < 0 || it >= legs.size }
            ?.let { legs[it].first }

    fun arrivalTimeAtStation(station: Int) =
        stations.indexOf(station).minus(1)
            .takeUnless { it < 0 }
            ?.let { legs[it].second }

    fun departTimeAtStationIndex(index: Int) =
        legs[index].first

    fun arrivalTimeAtStationIndex(index: Int) =
        legs[index - 1].second
}

/**
 * @property trips          Index of trip in their master list.
 */
internal data class Station(
    val trips: List<Int>,
)

internal data class Journey(
    val legs: List<Leg>,
    val finalStation: Int,
) {
    internal data class Leg(
        val originStation: Int,
        val trip: Int,
    )
}