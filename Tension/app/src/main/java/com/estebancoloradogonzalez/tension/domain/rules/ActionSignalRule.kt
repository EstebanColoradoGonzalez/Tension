package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ActionSignal
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification

object ActionSignalRule {

    private const val WEIGHT_TOLERANCE = 0.01

    /**
     * Signal of the post-session summary.
     *
     * Since HU-46 a [rangeSignal] — the reading of the pair against the 8–12 range —
     * **prevails** over the comparison with the previous session, also on the first session:
     * the range needs no history to say «lower» or «1 de 4» (CA-46.01). It is `null` for
     * isometric exercises, deload sessions and pairs that have no weight signal at all
     * (CA-46.15, CA-46.16, CA-46.18), and then the reading of always applies.
     */
    fun resolve(
        classification: ProgressionClassification?,
        prescribedLoadKg: Double?,
        avgWeightKg: Double,
        routineRequiresDeload: Boolean,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        totalReps: Int,
        previousTotalReps: Int?,
        setCount: Int,
        isMastered: Boolean,
        isDeload: Boolean = false,
        rangeSignal: RangeSignal? = null,
    ): ActionSignal {
        if (isDeload) return ActionSignal.DeloadSession
        if (rangeSignal != null && !isIsometric) {
            return fromRange(rangeSignal, prescribedLoadKg ?: avgWeightKg)
        }
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
                if (routineRequiresDeload) {
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

    private fun fromRange(signal: RangeSignal, targetKg: Double): ActionSignal = when (signal) {
        is RangeSignal.Decrease -> ActionSignal.DecreaseLoad
        is RangeSignal.Increase -> ActionSignal.IncreaseLoad(targetKg, signal.streak)
        is RangeSignal.AddExtraWeight -> ActionSignal.AddExtraWeight(signal.streak)
        is RangeSignal.Progressing -> ActionSignal.TopOfRange(signal.streak)
        is RangeSignal.InRange -> ActionSignal.WithinRange
    }
}
