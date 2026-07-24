package com.jeffreyalanwang.dutchrailways.backend.server.repository

import com.jeffreyalanwang.dutchrailways.backend.server.dto.PassServiceTimetable
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.graphql.data.GraphQlRepository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.stream.Stream
import kotlin.streams.asSequence

val TIME_ZONE = ZoneId.of("Europe/Amsterdam")

@GraphQlRepository
interface PassServiceRepository     : JpaRepository<PassService, Int> {

    @Query("""select s from Stop s where s.serviceId = ?1""")
    fun getStops(passServiceId: Int): List<Stop>

    @Query("select s from Stop s where s.serviceId in ?1")
    fun getStops(passServiceId: List<Int>): Stream<Stop>

    @Query("""
        select s from Stop s
        where s.serviceId = ?1 
          and (s.arriveTime > ?2 or s.departTime > ?2) 
        order by s.arriveTime nulls first
        fetch first ?3 rows only
    """)
    fun getStops(passServiceId: Int, arriveOrDepartAfter: Instant, count: Int): List<Stop>

    @Query("select s from Stop s where s.serviceId = ?1 and s.stationId = ?2")
    fun getStop(serviceId: Int, stationId: Int): Stop

    @Transactional
    fun getStop(serviceIdAndStationId: List<Pair<Int, Int>>): List<Stop> =
        getStops( serviceIdAndStationId.map { it.first } ) // Postgres database only builds an index on serviceId
            .asSequence()
            .joinedOn(keys = serviceIdAndStationId) { it.serviceId to it.stationId }
            .filterNotNull()

    @Suppress("FunctionName")
    @Query("select s from Stop s order by s.serviceId, s.arriveTime")
    fun _getAllStopsOrderByServiceThenTime(): Stream<Stop>

    @Transactional
    fun getAllTimetables(): List<PassServiceTimetable> =
        _getAllStopsOrderByServiceThenTime().asSequence()
            .deflattenBy { it.serviceId }
            .map { (k, v) -> PassServiceTimetable.fromStopEntities(v, id = k) }
            .toList()
}

@GraphQlRepository
interface AreaRepository            : JpaRepository<Area, Int>

@GraphQlRepository
interface StationRepository         : JpaRepository<Station, Int> {

    @Query("select id from Station")
    fun getAllStationIds(): List<Int>

    @Query("""
        select s from Stop s 
        where s.stationId = ?1 
        order by s.arriveTime nulls first
    """)
    fun getStops(stationId: Int): List<Stop>

    @Query("""
       select s from Stop s
        where s.stationId = ?1 
          and (s.arriveTime > ?2 or s.departTime > ?2) 
        order by s.arriveTime nulls first
        fetch first ?3 rows only
    """)
    fun getStops(stationId: Int, arriveOrDepartAfter: Instant, count: Int): List<Stop>

    fun getTimeZone(stationId: Int) = TIME_ZONE
    fun getTimeZone(stationId: List<Int>) = stationId.map { TIME_ZONE }

    fun Instant.atOffsetIn(stationId: Int): OffsetDateTime =
        atZone(getTimeZone(stationId)).toOffsetDateTime()

}
