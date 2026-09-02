package com.estebancoloradogonzalez.tension.domain.util

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import kotlin.math.max
import kotlin.math.round

/**
 * Converts load values between the capture unit and the canonical unit (kilograms).
 *
 * The converted value is never snapped to the system load increment: the increment
 * governs the capture controls, not the precision of the stored datum.
 */
object WeightConverter {

    const val LB_TO_KG = 0.45359237
    const val MAX_WEIGHT_KG = 500.0

    private const val STORAGE_DECIMALS = 100.0
    private const val CAPTURE_DECIMALS = 10.0

    /** Converts a captured value into kilograms, rounded to 2 decimals. */
    fun toKg(value: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> round(value * STORAGE_DECIMALS) / STORAGE_DECIMALS
            WeightUnit.LB -> round(value * LB_TO_KG * STORAGE_DECIMALS) / STORAGE_DECIMALS
        }
    }

    /** Converts a canonical value into the given capture unit, rounded to 1 decimal. */
    fun fromKg(weightKg: Double, unit: WeightUnit): Double {
        val value = when (unit) {
            WeightUnit.KG -> weightKg
            WeightUnit.LB -> weightKg / LB_TO_KG
        }
        return round(value * CAPTURE_DECIMALS) / CAPTURE_DECIMALS
    }

    /** Applies one step of the unit's capture increment. Never returns a negative value. */
    fun step(value: Double, unit: WeightUnit, increase: Boolean): Double {
        val delta = if (increase) unit.captureStep else -unit.captureStep
        val stepped = max(0.0, value + delta)
        return round(stepped * CAPTURE_DECIMALS) / CAPTURE_DECIMALS
    }
}
