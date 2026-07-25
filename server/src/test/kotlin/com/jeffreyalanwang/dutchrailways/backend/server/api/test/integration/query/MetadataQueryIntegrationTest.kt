package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import com.jeffreyalanwang.dutchrailways.backend.server.dto.AmenityEnum
import com.jeffreyalanwang.dutchrailways.backend.server.dto.TrainsetTypeEnum
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import java.time.ZoneId

@AutoConfigureHttpGraphQlTester
@SpringBootTest
class MetadataQueryIntegrationTest(
    @Autowired val graphQlTester: HttpGraphQlTester,
) {

    @Test
    fun `Time zone of station in Netherlands`() {

        @Language("GraphQL")
        val query = $$"""
            query TimeZoneOf($stationId: ID!) {
                timeZoneOf(stationId: $stationId)
            }
        """.trimIndent().let { '\n' + it + '\n' }

        val response = graphQlTester
            .document(query)
            .variable("stationId", 1176)
            .execute()

        response.path("timeZoneOf")
            .hasValue()
            .entity<String>()
            .matches { ZoneId.of(it) == ZoneId.of("Europe/Amsterdam") }

    }

    @Test
    fun `Default amenities`() {

        @Language("GraphQL")
        val query = $$"""
            query DefaultAmenitiesOf($trainset: Trainset!) {
                defaultAmenitiesOf(trainset: $trainset)
            }
        """.trimIndent()

        val response = graphQlTester
            .document(query)
            .variable("trainset", TrainsetTypeEnum.DDZ)
            .execute()

        response.path("defaultAmenitiesOf").entityList<AmenityEnum>()
            .hasSizeGreaterThan(1)

    }
}