package com.jeffreyalanwang.dutchrailways.backend.server.api.test.unit.controller.mutation

import com.jeffreyalanwang.dutchrailways.api.util.GeoCoords
import com.jeffreyalanwang.dutchrailways.backend.server.api.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.api.mutation.PlaceMutationController
import com.jeffreyalanwang.dutchrailways.backend.server.api.query.StationQueryController
import com.jeffreyalanwang.dutchrailways.backend.server.repository.AreaRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.StationRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Area
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Station
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.geolatte.geom.G2D
import org.geolatte.geom.MultiPolygon
import org.geolatte.geom.Point
import org.geolatte.geom.codec.Wkt
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import java.util.*

@GraphQlTest(PlaceMutationController::class, StationQueryController::class)
@Import(GraphQlConfig::class)
class PlaceMutationUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkBean private lateinit var areaRepository: AreaRepository
    @MockkBean private lateinit var stationRepository: StationRepository

    @Test
    fun `Update Area`() {

        val id = 0

        val oldName = "Old Name"
        val oldGeom = """
            MULTIPOLYGON(
              (
                (30 20, 45 40, 10 40, 30 20)
              ), 
              (
                (15 5, 40 10, 10 20, 5 10, 15 5)
              )
            )
        """.trimIndent().let { Wkt.fromWkt(it, WGS84) as MultiPolygon<G2D> }

        val newName = "New Name"

        @Language("GraphQL")
        val query = $$"""
            mutation UpdateArea($id: ID!, $details: MutationArea!) {
                updateArea(id: $id, details: $details) {
                    id
                    name
                }
            }
        """.trimIndent()

        every {
            areaRepository.findById(any())
        } returns Optional.of(Area(id = id, name = oldName, geom = oldGeom))

        every {
            areaRepository.save(any())
        } answers {
            firstArg()
        }

        val response = graphQlTester.document(query)
            .variable("id", id)
            .variable("details", mapOf(
                "name" to newName
            ))
            .execute()

        verify {
            areaRepository.findById(id)
            areaRepository.save(
                withArg {
                    check(it.id == id)
                    check(it.name == newName)
                    check(it.geom == oldGeom)
                }
            )
        }

        response
            .path("updateArea").hasValue()
            .path("updateArea.id").entity<Int>().isEqualTo(id)
            .path("updateArea.name").entity<String>().isEqualTo(newName)
    }

    @Test
    fun `Update Station`() {

        val id = 0

        val oldName = "Old Name"
        val oldAddress = "Old Address"
        val oldGeom = Wkt.fromWkt("POINT (-122.349 47.651)", WGS84) as Point<G2D>

        val newName = "New Name"
        val newAddress = "New Address"
        val newGeom = Wkt.fromWkt("POINT (32.842 10.333)", WGS84) as Point<G2D>

        @Language("GraphQL")
        val query = $$"""
            mutation UpdateStation($id: ID!, $details: MutationStation!) {
                updateStation(id: $id, details: $details) {
                    id
                    name
                    address
                    geom {
                        latitude
                        longitude
                    }
                }
            }
        """.trimIndent()

        every {
            stationRepository.findById(any())
        } returns Optional.of(
            Station(id, oldName, oldAddress, oldGeom)
        )

        every {
            stationRepository.save(any())
        } answers {
            firstArg()
        }

        val response = graphQlTester.document(query)
            .variable("id", id)
            .variable("details", mapOf(
                "name" to newName,
                "address" to newAddress,
                "geom" to newGeom.let { GeoCoords(it) }.run {
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                    )
                },
            ))
            .execute()

        verify {
            stationRepository.findById(id)
            stationRepository.save(
                withArg {
                    assertEquals(id, it.id)
                    assertEquals(newName, it.name)
                    assertEquals(newAddress, it.address)
                    assertEquals(newGeom, it.geom)
                }
            )
        }

        response
            .path("updateStation").hasValue()
            .path("updateStation.id").entity<Int>().isEqualTo(id)
            .path("updateStation.name").entity<String>().isEqualTo(newName)
            .path("updateStation.address").entity<String>().isEqualTo(newAddress)
            .path("updateStation.geom").hasValue()
            .path("updateStation.geom.latitude").entity<Double>().isEqualTo(newGeom.position.lat)
            .path("updateStation.geom.longitude").entity<Double>().isEqualTo(newGeom.position.lon)
    }
}