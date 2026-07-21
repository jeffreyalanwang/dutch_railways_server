package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.jeffreyalanwang.dutchrailways.backend.server.repository.*
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.QArea
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.QStation
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.QStop
import graphql.scalars.ExtendedScalars
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

@Configuration
class GraphQlConfig{
    @Bean
    fun annotatedControllerConfigurer(conversionService: ConversionService) =
        AnnotatedControllerConfigurer()
        .withBinderConfiguration {
            conversionService(conversionService)
        }

    @Bean
    fun runtimeWiringConfigurer(
        passServiceRepository: PassServiceRepository,
        placeRepository: PlaceRepository,
        areaRepository: AreaRepository,
        stationRepository: StationRepository,
        stopRepository: StopRepository,
    ) = RuntimeWiringConfigurer {
            scalar(ExtendedScalars.DateTime)
            configureType("Query") {
                "passServiceById" fetches
                    querydsl(passServiceRepository).single()

                "placeById" fetches
                    querydsl(placeRepository).single()

                "areaById" fetches
                    querydsl(areaRepository) { root: QArea ->
                        "id" binds root.id on defaultBinding
                    }.single()

                "stationById" fetches
                    querydsl(stationRepository) { root: QStation ->
                        "id" binds root.id on defaultBinding
                    }.single()

                "stopOfPassServiceAtStation" fetches
                    querydsl(stopRepository) { root: QStop ->
                        "service" binds root.serviceId on defaultBinding
                        "station" binds root.stationId on defaultBinding
                    }.single()
            }
        }
}

@Primary
@Component
class GraphQlConversionService (
    /**
     * A full instance of a Spring GraphQL application, by default, will use
     * [org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration.EnableWebMvcConfiguration].
     *
     * For tests, however, we inject a default [ConfigurableConversionService]
     * that does not require an entire running application's context:
     * [ApplicationConversionService].
     */
    private val delegate: ConfigurableConversionService = ApplicationConversionService(),
) : ConversionService by delegate {

    init {
        delegate.run {
            addConverter<OffsetDateTime, Instant> { it.toInstant().toKotlinInstant() }
        }
    }

    // Override Java default method from [ConversionService]
    override fun convert(source: Any?, targetType: TypeDescriptor) = delegate.convert(source, targetType)

}
