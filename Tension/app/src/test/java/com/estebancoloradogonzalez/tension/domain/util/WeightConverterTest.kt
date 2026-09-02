package com.estebancoloradogonzalez.tension.domain.util

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightConverterTest {

    // ----- toKg -----

    @Test
    fun `given 45 lb, when converted to kg, then result is 20 point 41`() {
        // Given
        val captured = 45.0

        // When
        val result = WeightConverter.toKg(captured, WeightUnit.LB)

        // Then
        assertEquals(20.41, result, 0.0)
    }

    @Test
    fun `given a value in kg, when converted to kg, then it is unchanged`() {
        assertEquals(60.0, WeightConverter.toKg(60.0, WeightUnit.KG), 0.0)
        assertEquals(62.5, WeightConverter.toKg(62.5, WeightUnit.KG), 0.0)
    }

    @Test
    fun `given a value in lb, when converted, then it is not snapped to a 0 point 5 multiple`() {
        // Given
        val captured = 45.0

        // When
        val result = WeightConverter.toKg(captured, WeightUnit.LB)

        // Then
        assertTrue("$result should not be a multiple of 0.5", (result * 2) % 1.0 != 0.0)
    }

    @Test
    fun `given a value in lb, when converted, then it keeps exactly two decimals`() {
        // 100 lb = 45.359237 kg
        assertEquals(45.36, WeightConverter.toKg(100.0, WeightUnit.LB), 0.0)
        // 1 lb = 0.45359237 kg
        assertEquals(0.45, WeightConverter.toKg(1.0, WeightUnit.LB), 0.0)
    }

    @Test
    fun `given zero, when converted from either unit, then result is zero`() {
        assertEquals(0.0, WeightConverter.toKg(0.0, WeightUnit.KG), 0.0)
        assertEquals(0.0, WeightConverter.toKg(0.0, WeightUnit.LB), 0.0)
    }

    // ----- fromKg -----

    @Test
    fun `given 20 point 41 kg, when converted back to lb, then result is 45 point 0`() {
        // Given
        val storedKg = 20.41

        // When
        val result = WeightConverter.fromKg(storedKg, WeightUnit.LB)

        // Then
        assertEquals(45.0, result, 0.0)
    }

    @Test
    fun `given a canonical value, when converted to kg, then it is rounded to one decimal`() {
        assertEquals(20.4, WeightConverter.fromKg(20.41, WeightUnit.KG), 0.0)
        assertEquals(20.4, WeightConverter.fromKg(20.44, WeightUnit.KG), 0.0)
        assertEquals(20.5, WeightConverter.fromKg(20.46, WeightUnit.KG), 0.0)
    }

    @Test
    fun `given any whole lb value, when it round trips through kg, then the lb value is preserved`() {
        for (lb in 1..1100) {
            val storedKg = WeightConverter.toKg(lb.toDouble(), WeightUnit.LB)
            val recovered = WeightConverter.fromKg(storedKg, WeightUnit.LB)
            assertEquals("round trip failed for $lb lb", lb.toDouble(), recovered, 0.0)
        }
    }

    // ----- step -----

    @Test
    fun `given kg unit, when stepped, then the value moves by 0 point 5`() {
        assertEquals(40.5, WeightConverter.step(40.0, WeightUnit.KG, increase = true), 0.0)
        assertEquals(39.5, WeightConverter.step(40.0, WeightUnit.KG, increase = false), 0.0)
    }

    @Test
    fun `given lb unit, when stepped, then the value moves by 1`() {
        assertEquals(46.0, WeightConverter.step(45.0, WeightUnit.LB, increase = true), 0.0)
        assertEquals(44.0, WeightConverter.step(45.0, WeightUnit.LB, increase = false), 0.0)
    }

    @Test
    fun `given a value below one step, when decreased, then the result is clamped at zero`() {
        assertEquals(0.0, WeightConverter.step(0.4, WeightUnit.KG, increase = false), 0.0)
        assertEquals(0.0, WeightConverter.step(0.0, WeightUnit.LB, increase = false), 0.0)
    }

    // ----- maximum boundary -----

    @Test
    fun `given the maximum, when expressed in lb, then it is 1102 point 3`() {
        // When
        val maxInLb = WeightConverter.fromKg(WeightConverter.MAX_WEIGHT_KG, WeightUnit.LB)

        // Then
        assertEquals(1102.3, maxInLb, 0.0)
    }

    @Test
    fun `given values around the maximum, when converted, then the boundary is respected`() {
        assertTrue(WeightConverter.toKg(500.0, WeightUnit.KG) <= WeightConverter.MAX_WEIGHT_KG)
        assertTrue(WeightConverter.toKg(1102.0, WeightUnit.LB) <= WeightConverter.MAX_WEIGHT_KG)
        assertTrue(WeightConverter.toKg(500.01, WeightUnit.KG) > WeightConverter.MAX_WEIGHT_KG)
        assertTrue(WeightConverter.toKg(1103.0, WeightUnit.LB) > WeightConverter.MAX_WEIGHT_KG)
    }
}
