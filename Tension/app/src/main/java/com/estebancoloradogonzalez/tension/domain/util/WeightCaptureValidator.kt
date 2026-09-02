package com.estebancoloradogonzalez.tension.domain.util

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit

/** Reason why a captured weight cannot be registered. */
sealed interface WeightCaptureError {
    data object NotNumeric : WeightCaptureError
    data object Negative : WeightCaptureError
    data object AboveMax : WeightCaptureError
}

/**
 * Validates the weight field of the set registration form.
 *
 * The range is always evaluated on the canonical value in kilograms, never on the
 * value as captured, so the same physical limit applies in both units.
 */
object WeightCaptureValidator {

    /** Returns null when the input is acceptable, or blank (nothing to report yet). */
    fun validate(rawInput: String, unit: WeightUnit): WeightCaptureError? {
        if (rawInput.isBlank()) return null

        val value = rawInput.toDoubleOrNull() ?: return WeightCaptureError.NotNumeric
        if (value < 0) return WeightCaptureError.Negative

        val weightKg = WeightConverter.toKg(value, unit)
        if (weightKg > WeightConverter.MAX_WEIGHT_KG) return WeightCaptureError.AboveMax

        return null
    }
}
