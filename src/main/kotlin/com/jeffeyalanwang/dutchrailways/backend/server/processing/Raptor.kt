package com.jeffeyalanwang.dutchrailways.backend.server.processing

import kotlin.time.Instant

class Raptor<ETrip, EStation> : RouteQueryStrategy<ETrip, EStation> {
    override fun invoke(
        origin: ETrip,
        destination: ETrip,
        startTime: Instant,
        stopDetails: Map<ETrip, GenericTripDetails<EStation>>,
    ): List<GenericJourneyDetails<ETrip, EStation>> {

        val (trips, stations, tripsKey, stationsKey) = toInternalRepresentation(stopDetails)

        val journeys = raptor(
            origin = tripsKey.indexOf(origin),
            destination = tripsKey.indexOf(destination),
            startTime = startTime,
            trips = trips,
            stations = stations,
        )

        return journeys.map {
            it.toGenericModel( stationsKey, tripsKey )
        }

    }
}

/**
 * Implementation of RAPTOR transit-routing algorithm, with modifications:
 *  * Optimized with marking and target pruning.
 *  * Does not use parallelism techniques.
 *  * Foot-paths are not considered.
 *  * Trips are independent, rather than grouped into routes.
 *    (This is because that is how it is represented in the database schema.)
 *  * Locality of reference is left up to the JVM to implicitly optimize.
 *    (See Appendix A from the link below.)
 * See: https://www.microsoft.com/en-us/research/wp-content/uploads/2012/01/raptor_alenex.pdf
 *
 * @param origin        An index in [stations].
 * @param destination   An index in [stations].
 * @param startTime     The earliest time (inclusive) to board a train from the origin.
 * @param trips         A master list of trips; they are identified by their indices.
 * @param stations      A master list of stations; they are identified by their indices.
 *
 * @return              Empty list if the journey is impossible.
 */
internal fun raptor(
    origin: Int,
    destination: Int,
    startTime: Instant,
    trips: Array<Trip>,
    stations: Array<Station>,
): List<Journey> {
    if (origin == destination) return listOf(Journey( emptyList(), destination ))

    // Indices correspond to [k - 1]. (See [round])
    val parentStations = mutableListOf< Array<Int?> >()

    val labelsByK = mutableListOf(
        Array(stations.size) { if (it == origin) startTime else null }
    )

    var lastRoundMarked = setOf(origin)

    // Perform round for each allowed number of trips `k`.
    // Continue until no labels have been improved.
    while (true) {
        labelsByK.run {
            add(last().copyOf())
        }
        val results =
            raptorRound(
                labels = labelsByK.last(),
                prevMarked = lastRoundMarked,
                bestTargetTime = labelsByK.last()[destination],
                trips = trips,
                stations = stations,
            )
        if (results.marked.none()) break
        lastRoundMarked = results.marked
        parentStations.add(results.parentStations)
    }

    return reconstructJourney(destination, labelsByK, parentStations, trips, stations)
}

/**
 * A single round of the RAPTOR algorithm.
 *
 * Calculates the earliest possible arrival time to each station using
 * `k` trips, given the earliest possible arrival times to each station
 * using `k - 1` trips.
 *
 * @param labels            Earliest possible arrival to each station from
 *                          the origin using maximum `k - 1` trips.
 *                          Is modified as a side effect.
 *                          `null`: Represents unknown/infinite arrival time
 *                          (rather than using [Instant.DISTANT_FUTURE]).
 * @param prevMarked        Stations with improved arrival from the last round (`k - 1`).
 * @param bestTargetTime    Earliest currently known arrival to the destination.
 * @param trips             Full set of all trips.
 * @param stations          Full set of all stations.
 *
 * @see raptor
 * @see RoundResults
 */
internal fun raptorRound(
    labels: Array<Instant?>,
    prevMarked: Set<Int>,
    bestTargetTime: Instant?,
    trips: Array<Trip>,
    stations: Array<Station>,
): RoundResults {
    val marked = mutableSetOf<Int>()
    val parentStations = Array<Int?>(labels.size) { null }

    /**
     * Key:     ID (i.e., master index) of a trip containing at least one marked station.
     * Value:   Index within the trip's route of the earliest marked station (i.e., the hop-on station).
     */
    val prevMarkedTrips = buildMap<Int, Int> {
        for ((stationId, station) in prevMarked.run { this zip map { stations[it] } }) {
            for ((tripId, trip) in station.trips.run { this zip map { trips[it] } }) {
                if (stationId == trip.stations.last()) continue // the last stop cannot be a useful hop-on

                val stationIndex = trip.stations.indexOf(stationId)

                val existingMapValue = this@buildMap[tripId]
                if (existingMapValue != null && existingMapValue <= stationIndex) continue // we already found an earlier stop to hop on

                val improvedStationArrivalTime = labels[stationId]!!
                if (trip.departTimeAtStationIndex(stationIndex) < improvedStationArrivalTime) continue // station's new time still too late to allow hop-on onto this trip

                this@buildMap[tripId] = stationIndex
            }
        }
    }

    for ((tripId, hopOnIndex) in prevMarkedTrips) {
        val trip = trips[tripId]
        val hopOnStation = trip.stations[hopOnIndex]

        // Traverse the rest of the trip,
        // updating labels and parents if arrival time is better.

        for ( iStop in (hopOnIndex + 1)..trip.stations.lastIndex ) {
            val station = trip.stations[iStop]
            val arrivalTime = trip.arrivalTimeAtStationIndex(iStop)

            // Target pruning: this (and all further stops) are already behind our best time to the destination
            if (bestTargetTime finiteAndLt arrivalTime) break

            // If [arrivalTime] improves the previous label
            if (arrivalTime finiteAndLt labels[station]) {
                labels[station] = arrivalTime
                marked += station
                parentStations[station] = hopOnStation
            }
        }

    }

    return RoundResults(marked, parentStations)
}

/**
 * Return values of [raptorRound] at round `k`
 * (except the labels, which are modified as a side effect).
 */
internal class RoundResults(
    /**
     * Stations whose earliest arrival was improved during this round.
     */
    val marked: Set<Int>,

    /**
     * At station `i` as index, the station just before arriving at `i`.
     * `null`: When corresponding label was not an improvement in this round.
     */
    val parentStations: Array<Int?>,
)

/**
 * Recreate the journey to [destination].
 *
 * Note that, because a trip's arrival time cannot be improved in k + 1
 * rounds without improvement in k rounds, we can assume that the parent
 * station is not null during any round except the first and last.
 *
 * @param labels            Labels from each round.
 * @param parentStations    Parent trips from each round except the first.
 *                          `parentStation[i]` corresponds to `labels[i - 1]`.
 * @param stations          Full set of all stations.
 *
 * @return `null` if [destination] does not have any parent station/trip.
 */
internal fun reconstructJourney(
    destination: Int,
    labels: List<Array<Instant?>>,
    parentStations: List<Array<Int?>>,
    trips: Array<Trip>,
    stations: Array<Station>,
): List<Journey> {
    if (labels.last()[destination] == null) return emptyList()

    // The algorithm continues searching until it is certain that the
    // target arrival is not going to improve; remove data from those
    // excess rounds.
    val labels = labels
        .dropLastWhile { it[destination] == labels.last()[destination] }
        .plusElement(labels.last())
    val parentStations = parentStations.take(labels.size - 1)
    // From this point, every parent station value on the path between
    // the origin and the destination is non-null.

    var currStation = destination
    var currArrivalTime = labels.last()[destination]!!
    val legs = buildList {
        for ((prevRoundLabels, parentStations) in (labels.dropLast(1) zip parentStations).reversed()) {
            val parentStation = parentStations[currStation]!!
            val parentArrivalTime = prevRoundLabels[parentStation]!!

            val arrivalPossible = stations[currStation].trips
                .filter {
                    val arrivalTime = trips[it].arrivalTimeAtStation(currStation)
                    arrivalTime != null && arrivalTime == currArrivalTime
                }
            val departurePossible = stations[parentStation].trips
                .filter {
                    val departTime = trips[it].departTimeAtStation(parentStation)
                    departTime != null && departTime in parentArrivalTime..<currArrivalTime
                }
            val possibleTrips = departurePossible.toSet() intersect arrivalPossible.toSet()

            add(
                possibleTrips.map {
                    Journey.Leg(
                        originStation = parentStation,
                        trip = it,
                    )
                }
            )

            currStation = parentStation
            currArrivalTime = parentArrivalTime
        }
    }

    return legs.cartesianProduct()
        .map {
            Journey(
                finalStation = destination,
                legs = it,
            )
        }
}