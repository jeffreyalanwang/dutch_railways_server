package com.jeffreyalanwang.dutchrailways.backend.server.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(GraphQlController::class)
@Import(GraphQlConfig::class, GraphQlConversionService::class)
class MutationUnitTests {

    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Test
    fun `Create PassService`() {
        val query = $$"""
            mutation CreatePassService($details: MutationPassService!) {
                createPassService(details: $details) {
                    id
                    name
                }
            }
        """.trimIndent()

        graphQlTester.document(query)
            .variable("details", mapOf(
                "name" to "Sprinter 101",
                "trainset" to "SLT",
                "amenities" to "WIFI"
            ))
            .execute()
            .path("createPassService")
    }

    @Test
    fun `Update PassService`() = TODO() as Unit

    @Test
    fun `Delete PassService`() = TODO() as Unit

    @Test
    fun `Update Area`() = TODO() as Unit

    @Test
    fun `Update Station`() = TODO() as Unit
}