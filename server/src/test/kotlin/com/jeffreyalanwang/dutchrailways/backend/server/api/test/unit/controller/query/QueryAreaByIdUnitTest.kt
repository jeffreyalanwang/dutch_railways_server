package com.jeffreyalanwang.dutchrailways.backend.server.api.test.unit.controller.query

import com.jeffreyalanwang.dutchrailways.backend.server.api.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.api.query.AreaQueryController
import com.jeffreyalanwang.dutchrailways.backend.server.repository.AreaRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import io.mockk.verify
import org.geolatte.geom.G2D
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.codec.Wkt
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entityList
import kotlin.collections.map
import kotlin.test.Test

@GraphQlTest(AreaQueryController::class)
@Import(GraphQlConfig::class)
class QueryAreaByIdUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkSpyBean private lateinit var controller: AreaQueryController
    @MockkBean private lateinit var areaRepository: AreaRepository

    @Language("GraphQL")
    val query = $$"""
        query AreaById($id: ID!) {
            areaById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @Test
    fun `Area returns geometry information`() {

        val id = 1
        val geom = """
            MULTIPOLYGON (
              (
                (0 0, 0 10, 10 10, 10 0, 0 0),
                (2 2, 8 2, 8 8, 2 8, 2 2),
                (4 4, 6 4, 6 6, 4 6, 4 4)
              ),
              (
                (20 0, 20 10, 30 10, 30 0, 20 0),
                (22 2, 28 2, 28 8, 22 8, 22 2),
                (24 4, 26 4, 26 6, 24 6, 24 4)
              ),
              (
                (40 0, 40 10, 50 10, 50 0, 40 0),
                (42 2, 48 2, 48 8, 42 8, 42 2),
                (44 4, 46 4, 46 6, 44 6, 44 4)
              )
            )
        """.trimIndent()
           .let { Wkt.fromWkt(it, WGS84) as MultiPolygon<G2D> }

        @Language("GraphQL")
        val fragment = """
            fragment Selection on Area {
                geom {
                    polygons {
                        rings {
                            points {
                                latitude
                                longitude
                            }                    
                        }
                    }
                }
            }
        """.trimIndent()

        every {
            areaRepository.findAllById(any())
        } answers {
            firstArg<List<Int>>().map { Area(id = it, geom = geom) }
        }

        val response = graphQlTester
            .document(query)
            .variable("id", id)
            .fragment(fragment)
            .execute()

        verify { areaRepository.findAllById(listOf(id)) }

        response.path("areaById").hasValue()
        response.path("areaById.geom").hasValue()
        response.path("areaById.geom.polygons[*]").hasValue()
        response.path("areaById.geom.polygons[*].rings").hasValue()
        response.path("areaById.geom.polygons[*].rings[*].points").hasValue()

        response.path("areaById.geom.polygons[*].rings[*].points[*].latitude")
            .entityList<Double>().containsExactly(
                *geom.flatMap { polygon ->
                    polygon.flatMap { ring ->
                        ring.positions.map { it.lat }
                    }
                }.toTypedArray()
            )
        response.path("areaById.geom.polygons[*].rings[*].points[*].longitude")
            .entityList<Double>().containsExactly(
                *geom.positions.map { it.lon }.toTypedArray()
            )
    }

}