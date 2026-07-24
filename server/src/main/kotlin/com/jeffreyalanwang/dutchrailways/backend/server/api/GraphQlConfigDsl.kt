package com.jeffreyalanwang.dutchrailways.backend.server.api

import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.asFlux
import org.dataloader.BatchLoaderEnvironment
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.data.querydsl.binding.QuerydslBindings
import org.springframework.graphql.data.GraphQlArgumentBinder
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerDetectionSupport
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import reactor.core.publisher.Flux
import kotlin.experimental.ExperimentalTypeInference


/**
 * Configure options for binding GraphQL arguments to target objects.
 *
 * Kotlin syntactic sugar for [configureBinder],
 * enabling use of configuration receiver object in lambda
 * and [apply]-style receiver return.
 *
 * While it would make more sense to simply overload [configureBinder],
 * this would be shadowed by the original method, making it impossible to call.
 */
internal fun <T: AnnotatedControllerDetectionSupport<*>> T.withBinderConfiguration(
    block: GraphQlArgumentBinder.Options.() -> Unit,
) = apply { configureBinder { it.block() } }

internal var GraphQlArgumentBinder.Options.conversionService
    get() = conversionService()
    set(value) { conversionService(value) }

internal inline fun <reified S : Any, reified T : Any> ConfigurableConversionService.addConverter(
    conversionService: Converter<S, T>,
) = addConverter(S::class.java, T::class.java, conversionService)

internal inline fun <reified K: Any, reified V: Any> BatchLoaderRegistry.forTypePair() = forTypePair(K::class.java, V::class.java)

internal fun <K: Any, V: Any> BatchLoaderRegistry.RegistrationSpec<K, V>.registerBatchLoader(
    loader: BatchLoaderEnvironment.(List<K>) -> Flow<V>,
) = registerBatchLoader { ids, environment -> environment.loader(ids).asFlux() }

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("registerBatchLoaderSynchronous")
internal fun <K: Any, V: Any> BatchLoaderRegistry.RegistrationSpec<K, V>.registerBatchLoader(
    loader: BatchLoaderEnvironment.(List<K>) -> List<V>,
) = registerBatchLoader { ids, environment -> environment.loader(ids).let { Flux.fromIterable(it) } }

internal fun RuntimeWiringConfigurer(
    block: RuntimeWiring.Builder.() -> Unit,
) = RuntimeWiringConfigurer { it.block() }

context(wiringBuilder: RuntimeWiring.Builder)
internal fun configureType(
    typename: String,
    block: TypeRuntimeWiring.Builder.() -> Unit,
) = wiringBuilder.type(typename) { it.block(); it }

context(wiringBuilder: TypeRuntimeWiring.Builder)
internal infix fun String.fetches(
    dataFetcher: DataFetcher<*>,
) = wiringBuilder.dataFetcher(this, dataFetcher)

context(bindings: QuerydslBindings)
internal val defaultBinding get() = null

