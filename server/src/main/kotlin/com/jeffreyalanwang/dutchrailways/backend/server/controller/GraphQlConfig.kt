package com.jeffreyalanwang.dutchrailways.backend.server.controller

import graphql.scalars.ExtendedScalars
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer
import java.time.ZoneId

@Configuration
class GraphQlConfig {

    @ConditionalOnMissingBean
    @Bean
    fun configurableConversionService(): ConfigurableConversionService = ApplicationConversionService()

    @Bean
    fun annotatedControllerConfigurer(configurableConversionService: ConfigurableConversionService) =
        AnnotatedControllerConfigurer()
            .withBinderConfiguration {
                conversionService = configurableConversionService.apply {
                    addConverter<ZoneId, String> { it.toString() }
                }
            }

    @Bean
    fun runtimeWiringConfigurer() = RuntimeWiringConfigurer {
        scalar(ExtendedScalars.DateTime)
    }

}
