package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.util.WeightConverter

/**
 * The values a set may carry — weight, repetitions (seconds held, on isometrics) and RIR.
 *
 * It exists so that registering a set (HU-06) and correcting the last one (HU-47) apply
 * **exactly the same** validations (CA-47.06). Two copies of these `require` would be free
 * to drift apart, and nothing — no test of either use case alone — would notice.
 *
 * [weightKg] is always the canonical value: the caller converts from the capture unit
 * first, so the range is checked on kilograms whatever unit the executant typed in.
 *
 * [equipmentTypeName] is only read for the one load rule the name carries: `Peso Añadido`
 * means external ballast, and ballast of zero is not ballast (CA-39.05).
 */
object SetValuesRule {

    const val MIN_REPS = 1
    val RIR_RANGE = 0..2

    /** Throws [IllegalArgumentException] with an internal message if any value is invalid. */
    fun requireValid(
        weightKg: Double,
        reps: Int,
        rir: Int,
        equipmentTypeName: String?,
    ) {
        require(weightKg >= 0) { "Weight must be >= 0" }
        require(weightKg <= WeightConverter.MAX_WEIGHT_KG) { "Weight must be <= 500 kg" }
        require(reps >= MIN_REPS) { "Reps must be >= 1" }
        require(rir in RIR_RANGE) { "RIR must be between 0 and 2" }
        if (ExternalLoadRule.requiresPositiveLoad(equipmentTypeName)) {
            require(weightKg > 0) { "Added weight must be > 0" }
        }
    }
}
