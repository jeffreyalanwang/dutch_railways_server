package com.jeffreyalanwang.util.geolatte.crs

import org.geolatte.geom.Geometry
import org.geolatte.geom.Position
import org.locationtech.proj4j.ProjCoordinate
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

infix fun Double.isNear(other: Double): Boolean {
    val compareMostSignificant = 3 // compare the 3 most significant decimal places from either of the two

    val maxDigitCountAboveZero = max(
        log10(this.absoluteValue).toInt(),
        log10(other.absoluteValue).toInt(),
    ) + 1
    val adjustment = -maxDigitCountAboveZero + compareMostSignificant // if positive: number of digits to move from right of the decimal to the left

    val diff = this - other
    return (diff * 10.0.pow(adjustment)).toInt() == 0
}

fun assertNear(expected: Double, actual: Double) {
    if (!(expected isNear actual))
        assertEquals(expected, actual) // easy way to make the error message
}

/**
 * @param checkZ Defaults to `false`. We do not use this and it is appearing to produce arbitrary values
 */
fun assertNear(expected: ProjCoordinate, actual: ProjCoordinate, checkZ: Boolean = false) {
    assertNear(expected.x, actual.x)
    assertNear(expected.y, actual.y)
    if (checkZ) assertNear(expected.z, actual.z)
}

fun assertNear(expected: Position, actual: Position) {
    assertEquals(expected.coordinateDimension, actual.coordinateDimension)
    for (i in 0 ..< expected.coordinateDimension) {
        assertNear(expected.getCoordinate(i), actual.getCoordinate(i))
    }
}

fun <P : Position> assertNear(expected: Geometry<P>, actual: Geometry<P>) {
    assertEquals(expected.geometryType, actual.geometryType)
    assertEquals(expected.coordinateReferenceSystem, actual.coordinateReferenceSystem)
    assertEquals(expected.numPositions, actual.numPositions)
    for (i in 0 ..< expected.numPositions) {
        assertNear(expected.getPositionN(i), actual.getPositionN(i))
    }
}