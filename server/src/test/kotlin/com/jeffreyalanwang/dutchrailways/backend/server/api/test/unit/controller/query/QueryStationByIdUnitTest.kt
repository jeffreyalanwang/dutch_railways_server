package com.jeffreyalanwang.dutchrailways.backend.server.api.test.unit.controller.query

import com.jeffreyalanwang.dutchrailways.backend.server.api.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.api.StationQueryController
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import io.mockk.verify
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.geolatte.geom.crs.CoordinateReferenceSystems
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import kotlin.collections.map
import kotlin.test.Test

@GraphQlTest(StationQueryController::class)
@Import(GraphQlConfig::class)
class QueryStationByIdUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkSpyBean private lateinit var controller: StationQueryController
    @MockkBean private lateinit var stationRepository: StationRepository

    @Language("GraphQL")
    val query = $$"""
        query StationById($id: ID!) {
            stationById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @Test
    fun `Station returns geometry information`() {

        val id = 1229
        val latitude = 5.0
        val longitude = 6.0

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Station {
                geom {
                    latitude
                    longitude
                }
            }
        """.trimIndent()

        val point = Point(
            G2D(longitude, latitude),
            CoordinateReferenceSystems.WGS84,
        )

        every {
            stationRepository.findAllById(any())
        } answers {
            firstArg<List<Int>>().map { Station(id = it, geom = point) }
        }

        val response = graphQlTester
            .document(query)
            .variable("id", id)
            .fragment(fragment)
            .execute()

        verify { stationRepository.findAllById(listOf(id)) }

        response.path("stationById.geom")
            .hasValue()
            .entity<Map<String, Any>>()
            .get().let { print(it) }

        response.path("stationById.geom.latitude")
            .hasValue()
            .entity<Double>()
            .isEqualTo(latitude)

        response.path("stationById.geom.longitude")
            .hasValue()
            .entity<Double>()
            .isEqualTo(longitude)
    }

}