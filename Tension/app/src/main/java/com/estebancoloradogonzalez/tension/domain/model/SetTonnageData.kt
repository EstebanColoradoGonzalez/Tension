package com.estebancoloradogonzalez.tension.domain.model

import com.estebancoloradogonzalez.tension.domain.rules.RealLoadRule

/**
 * One set, once per muscle group it counts for, with what the tonnage needs.
 *
 * [weightKg] is the external load as registered. The fields after [muscleGroup] are the
 * ingredients of the real load (HU-49): they default to «adds nothing» so that a row without
 * them weighs exactly its external load.
 */
data class SetTonnageData(
    val weightKg: Double,
    val reps: Int,
    val muscleGroup: String,
    /** Body weight in force on the session date, or null if the history has none that early. */
    val bodyWeightKg: Double? = null,
    val bodyWeightPercent: Int = 0,
    val isIsometric: Boolean = false,
    val equipmentTypeName: String? = null,
) {
    /** The load the tonnage multiplies by the repetitions (CA-49.01). */
    val realLoadKg: Double
        get() = RealLoadRule.realLoadKg(
            externalLoadKg = weightKg,
            bodyWeightKg = bodyWeightKg,
            bodyWeightPercent = bodyWeightPercent,
            isIsometric = isIsometric,
            equipmentTypeName = equipmentTypeName,
        )
}
