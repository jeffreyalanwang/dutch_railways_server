package com.jeffreyalanwang.dutchrailways.backend.routeQuery

import com.jeffreyalanwang.dutchrailways.backend.routeQuery.impl.raptor.Raptor
import com.jeffreyalanwang.dutchrailways.backend.routeQuery.model.external.GenericTripDetails
import kotlinx.datetime.*
import kotlinx.datetime.format.char
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private fun String.parseTime() =
    LocalDateTime.Format { date(LocalDate.Formats.ISO); char(' '); time(LocalTime.Formats.ISO) }
    .parse(this).toInstant(UtcOffset.ZERO)

private class DbStop(val passServiceId: Int, arriveTime: String?, departTime: String?, val stationId: Int) {
    val arriveTime = arriveTime?.run { parseTime() }
    val departTime = departTime?.run { parseTime() }
}

class RaptorTest2 {

    private val trips = listOf(
        DbStop(547, null                       ,"2026-05-02 18:38:00.000000",1153),
        DbStop(547,"2026-05-02 18:49:00.000000","2026-05-02 18:51:00.000000",1260),
        DbStop(547,"2026-05-02 19:01:00.000000","2026-05-02 19:03:00.000000",1162),
        DbStop(547,"2026-05-02 19:16:00.000000","2026-05-02 19:18:00.000000",1186),
        DbStop(547,"2026-05-02 19:44:00.000000","2026-05-02 19:48:00.000000",1198),
        DbStop(547,"2026-05-02 20:04:00.000000","2026-05-02 20:04:30.000000",1247),
        DbStop(547,"2026-05-02 20:14:00.000000","2026-05-02 20:14:30.000000",1209),
        DbStop(547,"2026-05-02 20:27:00.000000","2026-05-02 20:27:30.000000",1206),
        DbStop(547,"2026-05-02 20:44:00.000000", null                       ,1184),

        DbStop(574, null                       ,"2026-05-02 18:05:00.000000",1156),
        DbStop(574,"2026-05-02 18:13:00.000000","2026-05-02 18:13:30.000000",1208),
        DbStop(574,"2026-05-02 18:23:00.000000","2026-05-02 18:24:00.000000",1176),
        DbStop(574,"2026-05-02 18:42:00.000000","2026-05-02 18:51:00.000000",1158),
        DbStop(574,"2026-05-02 19:04:00.000000","2026-05-02 19:06:00.000000",1164),
        DbStop(574,"2026-05-02 19:41:00.000000","2026-05-02 19:46:00.000000",1198),
        DbStop(574,"2026-05-02 20:25:00.000000","2026-05-02 20:25:30.000000",1168),
        DbStop(574,"2026-05-02 20:42:00.000000", null                       ,1177),
    )

    @Test
    fun `Ensure correct leg order in final output`() {
        val origin = 1176
        val destination = 1247
        val startTime = trips.minOf { it.departTime?.minus(1.days) ?: Instant.DISTANT_FUTURE }

        val map = trips.groupBy { it.passServiceId }.mapValues { (trip, stops) ->
            GenericTripDetails(
                stations = stops.map { it.stationId },
                times = stops.zipWithNext { stop1, stop2 ->
                    GenericTripDetails.Leg(stop1.departTime!!, stop2.arriveTime!!)
                }
            )
        }

        val graph = RouteQueryDataSource.fromTrips(map.keys, map.values)

        val results = with(graph) {
            Raptor(
                origin = origin,
                destination = destination,
                startTime = startTime,
            )
        }

        assertEquals(1, results.size)
        val journey = results.first()

        assertEquals(2, journey.legStartPoints.size)

        val stations = journey.legStartPoints.map { it.originStation } + journey.finalStation
        assertEquals(origin, stations.first())
        assertEquals(destination, stations.last())
    }

}