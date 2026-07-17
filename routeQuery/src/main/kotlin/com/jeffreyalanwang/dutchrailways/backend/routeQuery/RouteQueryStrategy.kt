package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.RouteQueryDataSource
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.StationId
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.graph.TransitGraph
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.internal.obj.Journey
import kotlin.time.Instant

abstract class RouteQueryStrategy {

    context(graph: TransitGraph)
    internal abstract operator fun invoke(
        origin: StationId,
        destination: StationId,
        startTime: Instant,
    ): List<Journey>

    context(dataSource: RouteQueryDataSource<ETrip, EStation>)
    operator fun <ETrip, EStation> invoke(
        origin: EStation,
        destination: EStation,
        startTime: Instant,
    ) = with(dataSource.dataConverter) {
            val origin = origin.convertToInternal()
            val destination = destination.convertToInternal()
            val result =
                with(dataSource.transitGraph) {
                    invoke(origin, destination, startTime)
                }
            result.convertToExternal()
        }
    
}