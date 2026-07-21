package com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.buildListReversed
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.cartesianProduct
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.finiteAndLt
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.GraphAttribute
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TripId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey
import kotlin.time.Instant

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
 *                          (rather than using [kotlin.time.Instant.Companion.DISTANT_FUTURE]).
 * @param prevMarkedStations        Stations with improved arrival from the last round (`k - 1`).
 * @param bestTargetTime    Earliest currently known arrival to the destination.
 *
 * @see raptor
 * @see RoundResults
 */
context(graph: TransitGraph)
internal fun raptorRound(
    labels: GraphAttribute<StationId, Instant?>,
    prevMarkedStations: Set<StationId>,
    bestTargetTime: Instant?,
): RoundResults = with (graph) {
    val marked = mutableSetOf<StationId>()
    val parentStations = GraphAttribute.new<StationId, StationId?> { null }

    /**
     * Key:     A trip containing at least one marked station.
     * Value:   Index within the trip's route of the earliest marked station (i.e., the hop-on station).
     */
    val prevMarkedTrips = buildMap<TripId, Int> {
        for (stationId in prevMarkedStations) {
            for ((tripId, trip) in graph[stationId].trips.withData()) {

                if (stationId == trip.stations.last())
                    // The last stop cannot be a useful hop-on
                    continue

                val stationIndex = trip.stations.indexOf(stationId)

                val existingEarliestStation = this[tripId]
                if (existingEarliestStation != null && existingEarliestStation <= stationIndex)
                    // We already found an earlier stop to hop on
                    continue

                if (trip.departTimeAt(stationIndex) < labels[stationId]!!)
                    // Station's new time still too late to allow hop-on onto this trip
                    continue

                this[tripId] = stationIndex
            }
        }
    }

    for ((tripId, hopOnIndex) in prevMarkedTrips) {
        val trip = graph[tripId]
        val hopOnStation = trip.stations[hopOnIndex]

        // Traverse the rest of the trip,
        // updating labels and parents if arrival time is better.

        val restOfTrip = (hopOnIndex + 1)..trip.stations.lastIndex
        for ((station, arriveTime) in restOfTrip.map { trip.stations[it] to trip.arriveTimeAt(it) }) {

            // Target pruning: this (and all further stops) are already behind our best time to the destination
            if (bestTargetTime finiteAndLt arriveTime) break

            // If [arriveTime] improves the previous label
            if (arriveTime finiteAndLt labels[station]) {
                labels[station] = arriveTime
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
    val marked: Set<StationId>,

    /**
     * At station `x`, the station just before arriving at `x`.
     * `null`: When corresponding label was not an improvement in this round.
     */
    val parentStations: GraphAttribute<StationId, StationId?>,
)

/**
 * Recreate the journey to [destination].
 *
 * Note that, because a trip's arrival time cannot be improved in k + 1
 * rounds without improvement in k rounds, we can assume that the parent
 * station is not null during any round except the first and last.
 *
 * @param labels            Labels from each round except the first.
 * @param parentStations    Parent trips from each round except the first.
 *
 * @return `null` if [destination] does not have any parent station/trip.
 */
context(graph: TransitGraph)
internal fun reconstructJourney(
    destination: StationId,
    labels: List<GraphAttribute<StationId, Instant?>>,
    parentStations: List<GraphAttribute<StationId, StationId?>>,
): List<Journey> {
    val destinationArriveTime = labels.last()[destination] ?: return emptyList()

    // The algorithm continues searching until it is certain that the
    // target arrival is not going to improve; remove data from those
    // excess rounds.
    val labels = labels
        .dropLastWhile { it[destination] == labels.last()[destination] }
    val parentStations = parentStations.take(labels.size)
    // From this point, every parent station value on the path between
    // the origin and the destination is non-null.

    var currStation = destination
    var currArriveTime: Instant = destinationArriveTime
    val legs = buildListReversed {
        for ((prevRoundLabels, parentStations) in (labels zip parentStations).reversed()) {
            val parentStation = parentStations[currStation]!!
            val parentArriveTime = prevRoundLabels[parentStation]!!

            val possibleTrips = graph[currStation].trips
                .filter { trip ->
                    val arriveTime = graph[trip].arriveTimeAt(currStation)
                    arriveTime != null && arriveTime == currArriveTime
                }
                .intersect(graph[parentStation].trips)
                .filter { trip ->
                    val departTime = graph[trip].departTimeAt(parentStation)
                    departTime != null && departTime in parentArriveTime..<currArriveTime
                }

            add(
                possibleTrips.map {
                    Journey.Leg(
                        originStation = parentStation,
                        trip = it,
                    )
                }
            )

            currStation = parentStation
            currArriveTime = parentArriveTime
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