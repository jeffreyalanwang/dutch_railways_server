package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Path
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer
import org.springframework.data.querydsl.binding.QuerydslBindings
import org.springframework.graphql.data.GraphQlArgumentBinder
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerDetectionSupport
import org.springframework.graphql.data.query.QuerydslDataFetcher
import org.springframework.graphql.execution.RuntimeWiringConfigurer


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

context(wiringBuilder: TypeRuntimeWiring.Builder)
internal fun <Q: EntityPath<T>, T: Any> querydsl(
    dataFetcher: QuerydslPredicateExecutor<T>,
    customizer: QuerydslBindings.(root: Q) -> Unit = {},
) = QuerydslDataFetcher.builder(dataFetcher)
    .customizer(
        QuerydslBinderCustomizer<Q> { bindings, root ->
            bindings.customizer(root)
        }
    )

context(bindings: QuerydslBindings)
internal infix fun <T: Path<S>, S: Any> String.binds(path: T) = bindings.bind(path).`as`(this)

context(bindings: QuerydslBindings)
internal infix fun <T: Path<out S>, S: Any> QuerydslBindings.AliasingPathBinder<T, S>.on(binding: Any?) =
    binding?.let { throw NotImplementedError() } ?: withDefaultBinding()

context(bindings: QuerydslBindings)
internal val defaultBinding get() = null

/**
 * Kotlin syntactic sugar for [addConverter],
 * enabling use of reified type parameters instead of Java class attributes.
 */
internal inline fun <reified S : Any, reified T : Any> ConfigurableConversionService.addConverter(
    conversionService: Converter<S, T>,
) = addConverter(S::class.java, T::class.java, conversionService)
