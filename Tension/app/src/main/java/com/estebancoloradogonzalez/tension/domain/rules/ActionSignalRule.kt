package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ActionSignal
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification

object ActionSignalRule {

    private const val WEIGHT_TOLERANCE = 0.01

    fun resolve(
        classification: ProgressionClassification?,
        prescribedLoadKg: Double?,
        avgWeightKg: Double,
        moduleRequiresDeload: Boolean,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        totalReps: Int,
        previousTotalReps: Int?,
        setCount: Int,
        isMastered: Boolean,
    ): ActionSignal {
        if (classification == null) return ActionSignal.FirstSession

        if (isIsometric) {
            if (isMastered) return ActionSignal.IsometricMastered
            val avgSeconds = if (setCount > 0) totalReps / setCount else 0
            return ActionSignal.IsometricSignal(setCount, avgSeconds)
        }

        if (isBodyweight) {
            val diff = previousTotalReps?.let { totalReps - it }
            return ActionSignal.BodyweightSignal(totalReps, diff)
        }

        // Standard (weighted) exercises
        if (prescribedLoadKg != null && prescribedLoadKg - avgWeightKg > WEIGHT_TOLERANCE) {
            return ActionSignal.IncreaseLoad(prescribedLoadKg)
        }

        return when (classification) {
            ProgressionClassification.REGRESSION -> {
                if (moduleRequiresDeload) {
                    ActionSignal.ConsiderDeload
                } else {
                    ActionSignal.MaintainLoad("Mantener carga")
                }
            }
            ProgressionClassification.POSITIVE_PROGRESSION -> ActionSignal.ProgressInReps
            ProgressionClassification.MAINTENANCE -> {
                ActionSignal.MaintainLoad("Mantener carga — progresar en reps")
            }
        }
    }
}
