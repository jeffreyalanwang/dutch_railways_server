package com.jeffreyalanwang.dutchrailways.backend.server.api.query

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class SearchQueryController(

) {
    @QueryMapping
    fun search
}