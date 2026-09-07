package com.estebancoloradogonzalez.tension.domain.model

/**
 * Value the set-registration form is prefilled with for one (exercise, implement) pair
 * (HU-40, CA-40.03).
 *
 * Both fields are resolved over the pair and not over the exercise: the prescribed load,
 * the memory of the last handled weight and the capture unit all belong to the implement.
 * When the pair has no history [weightKg] is null and the field stays empty — the weight of
 * another implement is never inherited.
 */
data class PrefilledLoad(
    val weightKg: Double?,
    val captureUnit: WeightUnit,
)
