package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.server.dto.PassServiceTimetable
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.*
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.graphql.data.GraphQlRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.time.Instant
import kotlin.time.toJavaInstant

// Repositories implementing [QuerydslPredicateExecutor] and annotated
// with [GraphQlRepository] are implicitly bound to GraphQL data fetchers
// which emulate [@SchemaMapping] and [@BatchMapping].
//
// For the other (explicit) bindings, [GraphQlConfig] and [GraphQlController].

@GraphQlRepository
interface PassServiceRepository     : JpaRepository<PassService, Int>   , QuerydslPredicateExecutor<PassService>

@GraphQlRepository
interface PlaceRepository           : JpaRepository<Place, Int>         , QuerydslPredicateExecutor<Place>

@GraphQlRepository
interface AreaRepository            : JpaRepository<Area, Int>          , QuerydslPredicateExecutor<Area>

@GraphQlRepository
interface StationRepository         : JpaRepository<Station, Int>       , QuerydslPredicateExecutor<Station> {

    @Query("select id from Station")
    fun getAllStationIds(): List<Int>

    fun getStationById(id: Int): Station

    @Query("select new Stop(serviceId, arriveTime, departTime, stationId) from Stop where stationId = ?1 limit 1")
    fun getArbitraryStop(stationId: Int): Stop

    // TODO this doesn't properly handle null times
    @Query("select new Stop(serviceId, arriveTime, departTime, stationId) from Stop where stationId = ?2 and departTime > ?1 order by arriveTime, departTime fetch first ?3 rows only")
    fun getStopsDepartingAfter(time: OffsetDateTime, stationId: Int, count: Int): List<Stop>

    fun getStopsDepartingAfter(time: Instant, stationId: Int, count: Int) =
        time.toJavaInstant().atOffset(ZoneOffset.UTC)
            .let { getStopsDepartingAfter(it, stationId, count) }

    fun Instant.atOffsetOf(stationId: Int): OffsetDateTime {
        val nearestStop = getStopsDepartingAfter(this, stationId = stationId, count = 1).firstOrNull()
            ?: getArbitraryStop(stationId)
        val nearestTime = nearestStop.run { arriveTime ?: departTime }!!
        return this.toJavaInstant().atOffset(nearestTime.offset)
    }
}

@GraphQlRepository
interface StopRepository            : JpaRepository<Stop, Int>          , QuerydslPredicateExecutor<Stop> {

    @Query("select new Stop(serviceId, arriveTime, departTime, stationId) from Stop where serviceId = ?1 and stationId = ?2")
    fun getStopByServiceAndStation(serviceId: Int, stationId: Int): Stop

    @Query("select new Stop(serviceId, arriveTime, departTime, stationId) from Stop where serviceId in ?1")
    fun getStopsByServiceIn(serviceId: List<Int>): Stream<Stop>

    @Transactional
    fun getStopsByServiceAndStation(keyList: List<Pair<Int, Int>>): List<Stop> =
        getStopsByServiceIn( keyList.map { it.first } ) // Postgres database only builds an index on serviceId
            .asSequence()
            .findInOrderIn(keyList) { it.serviceId to it.stationId }

    @Query("select new Stop(serviceId, arriveTime, departTime, stationId) from Stop order by serviceId, arriveTime")
    fun getInOrderByServiceThenTime(): Stream<Stop>

    @Transactional
    fun getAllPassServiceTimetables(): List<PassServiceTimetable> = getInOrderByServiceThenTime().asSequence()
        .deflattenBy { it.serviceId }
        .map { (k, v) -> PassServiceTimetable.fromStopEntities(v, id = k) }
        .toList()

}
