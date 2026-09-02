package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import kotlin.math.abs

object ProgressionClassificationRule {

    const val RIR_SIGNIFICANT_RISE = 1.5
    const val ISOMETRIC_MASTERED_THRESHOLD = 45
    private const val WEIGHT_TOLERANCE = 0.01

    fun classify(
        current: ExerciseSessionData,
        previous: ExerciseSessionData?,
        isBodyweight: Boolean,
        isIsometric: Boolean,
    ): ProgressionClassification? {
        if (previous == null || previous.sets.isEmpty()) return null
        if (current.sets.isEmpty()) return null

        return when {
            isIsometric -> classifyIsometric(current, previous)
            isBodyweight -> classifyBodyweight(current, previous)
            else -> classifyStandard(current, previous)
        }
    }

    fun isIsometricMastered(current: ExerciseSessionData): Boolean {
        return current.setCount >= 4 &&
            current.sets.all { it.reps >= ISOMETRIC_MASTERED_THRESHOLD }
    }

    /**
     * Resolves the new progression status and counter after a session is closed.
     *
     * [plateauThreshold] is the effective threshold of the exercise, resolved by the
     * caller through `PlateauThresholdRule` from the executant's base threshold and the
     * exercise's progression difficulty. It is intentionally required: a default would
     * let a new caller silently inherit an implicit threshold.
     *
     * The counter itself is threshold-agnostic — it always accumulates and only resets
     * on positive progression — so changing an exercise's difficulty re-evaluates the
     * condition without discarding what was already accumulated.
     */
    fun resolveNewProgressionState(
        currentStatus: String,
        currentCounter: Int,
        classification: ProgressionClassification?,
        isIsometric: Boolean,
        isMastered: Boolean,
        plateauThreshold: Int,
    ): Pair<String, Int> {
        if (isIsometric && isMastered) return "MASTERED" to 0
        if (classification == null) return currentStatus to currentCounter
        if (currentStatus == "IN_DELOAD") return currentStatus to currentCounter
        if (currentStatus == "MASTERED") return currentStatus to currentCounter

        return when (classification) {
            ProgressionClassification.POSITIVE_PROGRESSION -> "IN_PROGRESSION" to 0

            ProgressionClassification.MAINTENANCE,
            ProgressionClassification.REGRESSION -> {
                val newCounter = currentCounter + 1
                val newStatus = when {
                    newCounter >= plateauThreshold -> "IN_PLATEAU"
                    else -> currentStatus
                }
                newStatus to newCounter
            }
        }
    }

    private fun classifyStandard(
        current: ExerciseSessionData,
        previous: ExerciseSessionData,
    ): ProgressionClassification {
        val repsDiff = current.totalReps - previous.totalReps
        val rirRise = current.avgRir - previous.avgRir

        return when {
            isWeightLower(current.avgWeightKg, previous.avgWeightKg) ->
                ProgressionClassification.REGRESSION

            isWeightEqual(current.avgWeightKg, previous.avgWeightKg) && repsDiff < 0 ->
                ProgressionClassification.REGRESSION

            isWeightEqual(current.avgWeightKg, previous.avgWeightKg) &&
                rirRise >= RIR_SIGNIFICANT_RISE ->
                ProgressionClassification.REGRESSION

            isWeightHigher(current.avgWeightKg, previous.avgWeightKg) &&
                rirRise < RIR_SIGNIFICANT_RISE ->
                ProgressionClassification.POSITIVE_PROGRESSION

            isWeightEqual(current.avgWeightKg, previous.avgWeightKg) &&
                repsDiff > 0 && rirRise < RIR_SIGNIFICANT_RISE ->
                ProgressionClassification.POSITIVE_PROGRESSION

            else -> ProgressionClassification.MAINTENANCE
        }
    }

    private fun classifyBodyweight(
        current: ExerciseSessionData,
        previous: ExerciseSessionData,
    ): ProgressionClassification {
        val repsDiff = current.totalReps - previous.totalReps
        val rirRise = current.avgRir - previous.avgRir

        return when {
            repsDiff < 0 -> ProgressionClassification.REGRESSION
            rirRise >= RIR_SIGNIFICANT_RISE && repsDiff <= 0 ->
                ProgressionClassification.REGRESSION
            repsDiff > 0 && rirRise < RIR_SIGNIFICANT_RISE ->
                ProgressionClassification.POSITIVE_PROGRESSION
            else -> ProgressionClassification.MAINTENANCE
        }
    }

    private fun classifyIsometric(
        current: ExerciseSessionData,
        previous: ExerciseSessionData,
    ): ProgressionClassification {
        val secondsDiff = current.totalReps - previous.totalReps
        val rirRise = current.avgRir - previous.avgRir

        return when {
            secondsDiff < 0 -> ProgressionClassification.REGRESSION
            rirRise >= RIR_SIGNIFICANT_RISE && secondsDiff <= 0 ->
                ProgressionClassification.REGRESSION
            secondsDiff > 0 && rirRise < RIR_SIGNIFICANT_RISE ->
                ProgressionClassification.POSITIVE_PROGRESSION
            else -> ProgressionClassification.MAINTENANCE
        }
    }

    private fun isWeightEqual(a: Double, b: Double): Boolean =
        abs(a - b) < WEIGHT_TOLERANCE

    private fun isWeightHigher(a: Double, b: Double): Boolean =
        a - b >= WEIGHT_TOLERANCE

    private fun isWeightLower(a: Double, b: Double): Boolean =
        b - a >= WEIGHT_TOLERANCE
}
