package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey
import kotlin.time.Instant

abstract class RangeRouteQueryStrategy {

    /**
     * @param timeRange   Inclusive.
     */
    context(graph: TransitGraph)
    internal abstract operator fun invoke(
        origin: StationId,
        destination: StationId,
        timeRange: ClosedRange<Instant>,
    ): List<Journey>

    /** @throws com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.StationNotFoundException */
    context(dataSource: RouteQueryDataSource<ETrip, EStation>)
    operator fun <ETrip: Any, EStation: Any> invoke(
        origin: EStation,
        destination: EStation,
        timeRange: ClosedRange<Instant>,
    ) = with(dataSource.dataConverter) {
            val origin = origin.convertToInternal()
            val destination = destination.convertToInternal()
            val result =
                with(dataSource.transitGraph) {
                    invoke(origin, destination, timeRange)
                }
            result.convertToExternal()
        }

}