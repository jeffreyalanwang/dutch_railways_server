package com.jeffreyalanwang.dutchrailways.backend.server.api

import graphql.scalars.ExtendedScalars
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQlConfig {
    @Bean
    fun runtimeWiringConfigurer() = RuntimeWiringConfigurer {
        scalar(ExtendedScalars.DateTime)
    }
}
