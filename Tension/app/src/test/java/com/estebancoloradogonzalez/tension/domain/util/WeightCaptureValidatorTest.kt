package com.estebancoloradogonzalez.tension.domain.util

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightCaptureValidatorTest {

    @Test
    fun `given a blank input, when validated, then nothing is reported`() {
        assertNull(WeightCaptureValidator.validate("", WeightUnit.KG))
        assertNull(WeightCaptureValidator.validate("   ", WeightUnit.LB))
    }

    @Test
    fun `given a non numeric input, when validated, then NotNumeric is reported`() {
        assertEquals(
            WeightCaptureError.NotNumeric,
            WeightCaptureValidator.validate("abc", WeightUnit.KG),
        )
        assertEquals(
            WeightCaptureError.NotNumeric,
            WeightCaptureValidator.validate("1,5", WeightUnit.KG),
        )
        assertEquals(
            WeightCaptureError.NotNumeric,
            WeightCaptureValidator.validate("--3", WeightUnit.LB),
        )
    }

    @Test
    fun `given a negative input, when validated in either unit, then Negative is reported`() {
        assertEquals(
            WeightCaptureError.Negative,
            WeightCaptureValidator.validate("-1", WeightUnit.KG),
        )
        assertEquals(
            WeightCaptureError.Negative,
            WeightCaptureValidator.validate("-0.5", WeightUnit.LB),
        )
    }

    @Test
    fun `given zero, when validated, then it is accepted`() {
        assertNull(WeightCaptureValidator.validate("0", WeightUnit.KG))
        assertNull(WeightCaptureValidator.validate("0", WeightUnit.LB))
    }

    @Test
    fun `given 1200 lb, when validated, then AboveMax is reported on the converted value`() {
        // 1200 lb = 544.31 kg, above the 500 kg limit
        assertEquals(
            WeightCaptureError.AboveMax,
            WeightCaptureValidator.validate("1200", WeightUnit.LB),
        )
    }

    @Test
    fun `given 1100 lb, when validated, then it is accepted because it converts below the limit`() {
        // 1100 lb = 498.95 kg — proves the limit applies to the canonical value, not
        // to the number the executant typed
        assertNull(WeightCaptureValidator.validate("1100", WeightUnit.LB))
    }

    @Test
    fun `given the boundary in kg, when validated, then 500 passes and above it fails`() {
        assertNull(WeightCaptureValidator.validate("500", WeightUnit.KG))
        assertEquals(
            WeightCaptureError.AboveMax,
            WeightCaptureValidator.validate("500.01", WeightUnit.KG),
        )
    }

    @Test
    fun `given 600 kg, when validated as lb, then it is accepted`() {
        // The same number that fails in kilograms passes in pounds (272.16 kg)
        assertEquals(
            WeightCaptureError.AboveMax,
            WeightCaptureValidator.validate("600", WeightUnit.KG),
        )
        assertNull(WeightCaptureValidator.validate("600", WeightUnit.LB))
    }
}
