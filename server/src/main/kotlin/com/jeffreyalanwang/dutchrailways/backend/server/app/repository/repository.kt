package com.jeffreyalanwang.dutchrailways.backend.server.app.repository

import com.jeffreyalanwang.dutchrailways.backend.dataSource.Area
import com.jeffreyalanwang.dutchrailways.backend.dataSource.PassService
import com.jeffreyalanwang.dutchrailways.backend.dataSource.Place
import com.jeffreyalanwang.dutchrailways.backend.dataSource.Station
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.graphql.data.GraphQlRepository

@GraphQlRepository
interface PassServiceRepository     : JpaRepository<PassService, Int>   , QuerydslPredicateExecutor<PassService>

@GraphQlRepository
interface PlaceRepository           : JpaRepository<Place, Int>         , QuerydslPredicateExecutor<Place>

@GraphQlRepository
interface AreaRepository            : JpaRepository<Area, Int>          , QuerydslPredicateExecutor<Area>

@GraphQlRepository
interface StationRepository         : JpaRepository<Station, Int>       , QuerydslPredicateExecutor<Station>
