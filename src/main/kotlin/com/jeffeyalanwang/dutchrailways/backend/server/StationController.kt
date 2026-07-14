package com.jeffeyalanwang.dutchrailways.backend.server

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class StationController {
    @QueryMapping
    fun stationById(@Argument id: Int): Station =
        Station.byId(id)
}