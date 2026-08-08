package com.jeffreyalanwang.util.geolatte.crs

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream

abstract class CrsConversionTestDataProvider<T_C2D, T_G2D> : ArgumentsProvider {
    abstract fun provideTestValues(): Stream<CrsConversionTestValue<T_C2D, T_G2D>>
    override fun provideArguments(
        parameters: ParameterDeclarations,
        context: ExtensionContext,
    ): Stream<Arguments> = parameters.all.run {
        if (size != 2) {
            throw IllegalArgumentException("Can only supply 2 arguments")
        }
        if (all { it.parameterName.isPresent }) {
            require( map { it.parameterName.get() } == listOf("c2d", "g2d") )
        }
        provideTestValues()
            .map { testValue ->
                Arguments.arguments(testValue.c2d, testValue.g2d)
            }
    }
}