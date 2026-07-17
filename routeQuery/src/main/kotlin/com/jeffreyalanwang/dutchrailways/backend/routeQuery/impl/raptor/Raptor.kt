package com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.RouteQueryStrategy
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.GraphAttribute
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey
import kotlin.time.Instant

object Raptor: RouteQueryStrategy() {

    context(graph: TransitGraph)
    override fun invoke(
        origin: StationId,
        destination: StationId,
        startTime: Instant
    ) = raptor(origin, destination, startTime)

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
 * @param startTime     The earliest time (inclusive) to board a train from the origin.
 *
 * @return              Empty list if the journey is impossible.
 */
context(graph: TransitGraph)
internal fun raptor(
    origin: StationId,
    destination: StationId,
    startTime: Instant,
): List<Journey> {
    if (origin == destination) return listOf(Journey(emptyList(), destination))

    // Indices correspond to [k - 1]. (See [round])
    val parentStationsByK = mutableListOf<GraphAttribute<StationId, StationId?>>()

    val labelsByK = mutableListOf(
        GraphAttribute.new(origin to startTime) { null }
    )

    var lastRoundMarked = setOf(origin)

    // Perform round for each allowed number of trips `k`.
    // Continue until no labels have been improved.
    while (true) {
        labelsByK.run {
            add(last().copy())
        }
        val results =
            raptorRound(
                labels = labelsByK.last(),
                prevMarkedStations = lastRoundMarked,
                bestTargetTime = labelsByK.last()[destination],
            )
        if (results.marked.none()) break
        lastRoundMarked = results.marked
        parentStationsByK.add(results.parentStations)
    }

    return reconstructJourney(destination, labelsByK, parentStationsByK)
}

