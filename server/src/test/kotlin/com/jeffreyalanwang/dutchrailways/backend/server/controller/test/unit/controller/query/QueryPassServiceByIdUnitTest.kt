package com.jeffreyalanwang.dutchrailways.backend.server.controller.test.unit.controller.query

import com.jeffreyalanwang.dutchrailways.backend.server.controller.GraphQlConfig
import com.jeffreyalanwang.dutchrailways.backend.server.controller.query.PassServiceQueryController
import com.jeffreyalanwang.dutchrailways.api.Trainset
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.SetCompareBuilderScope.Companion.allSetEqual
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.TrainsetType
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import com.jeffreyalanwang.dutchrailways.api.Amenity as AmenityEnum
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Amenity as AmenityEntity

@GraphQlTest(PassServiceQueryController::class)
@Import(GraphQlConfig::class)
class QueryPassServiceByIdUnitTest {

    @Autowired private lateinit var graphQlTester: GraphQlTester
    @MockkSpyBean private lateinit var controller: PassServiceQueryController
    @MockkBean private lateinit var passServiceRepository: PassServiceRepository

    @Language("GraphQL")
    val query = $$"""
        query PassServiceById($id: ID!) {
            passServiceById(id: $id) {
                ...Selection
            }
        }
    """.trimIndent().let { '\n' + it + '\n' }

    @ParameterizedTest
    @EnumSource
    fun `Trainset enum`(trainsetType: Trainset) {

        val id = 470

        @Language("GraphQL")
        val fragment = """
            fragment Selection on PassService {
                trainset
            }
        """.trimIndent()

        every {
            passServiceRepository.findAllById(any())
        } answers {
            PassService(id = id, name = "")
                .apply {
                    consist = TrainsetType(
                        name = trainsetType.name,
                        amenities = mutableSetOf(),
                    )
                }
                .let { listOf(it) }
        }

        val response = graphQlTester
            .document(query)
            .variable("id", id)
            .fragment(fragment)
            .execute()

        verify {
            passServiceRepository.findAllById(listOf(id))
        }

        response.path("passServiceById.trainset")
            .hasValue()
            .entity<String>()
            .isEqualTo(trainsetType.name)
    }

    @Test
    fun `Amenities enum`() {

        val id = 470
        val amenities = AmenityEnum.entries

        @Language("GraphQL")
        val fragment = """
            fragment Selection on PassService {
                amenities
            }
        """.trimIndent()

        every {
            passServiceRepository.findAllById(any())
        } answers {
            PassService(id = id, name = "")
                .apply {
                    consist = TrainsetType(
                        name = "",
                        amenities = amenities.map {
                            AmenityEntity(
                                id = -1,
                                description = it.name,
                            )
                        }.toMutableSet(),
                    )
                }
                .let { listOf(it) }
        }

        val response = graphQlTester
            .document(query)
            .variable("id", id)
            .fragment(fragment)
            .execute()

        response.path("passServiceById.amenities")
            .hasValue()
            .entityList<String>()
            .matches<GraphQlTester.EntityList<String>> {
                allSetEqual {
                    amenities on { it.name }
                    it on itself
                }
            }
    }
}