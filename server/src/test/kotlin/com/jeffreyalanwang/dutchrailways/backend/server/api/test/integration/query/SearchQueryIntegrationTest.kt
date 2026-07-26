package com.jeffreyalanwang.dutchrailways.backend.server.api.test.integration.query

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester

@AutoConfigureHttpGraphQlTester
@SpringBootTest
class SearchQueryIntegrationTest {
    @Autowired val graphQlTester: HttpGraphQlTester,
) {
}