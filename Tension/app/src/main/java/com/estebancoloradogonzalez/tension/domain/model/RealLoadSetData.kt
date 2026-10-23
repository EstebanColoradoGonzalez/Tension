package com.estebancoloradogonzalez.tension.domain.model

import com.estebancoloradogonzalez.tension.domain.rules.RealLoadRule

/**
 * One set with the ingredients of its real load, once — the source of the session tonnage
 * (HU-49, CA-49.01).
 *
 * It exists apart from [SetTonnageData] because that one repeats the set for every muscle
 * group the exercise works: it serves the aggregation by group, not a session total.
 */
data class RealLoadSetData(
    val sessionId: Long,
    val weightKg: Double,
    val reps: Int,
    /** Body weight in force on the session date, or null if the history has none that early. */
    val bodyWeightKg: Double?,
    val bodyWeightPercent: Int,
    val isIsometric: Boolean,
    val equipmentTypeName: String?,
) {
    val realLoadKg: Double
        get() = RealLoadRule.realLoadKg(
            externalLoadKg = weightKg,
            bodyWeightKg = bodyWeightKg,
            bodyWeightPercent = bodyWeightPercent,
            isIsometric = isIsometric,
            equipmentTypeName = equipmentTypeName,
        )

    val tonnageKg: Double get() = realLoadKg * reps
}
