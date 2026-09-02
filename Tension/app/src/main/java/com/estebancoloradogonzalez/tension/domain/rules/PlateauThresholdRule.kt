package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import kotlin.math.ceil

/**
 * Resolves how many consecutive sessions without positive progression an exercise
 * must accumulate before it is declared to be in a plateau.
 *
 * Progression is uniform neither across exercises nor across people, so the threshold
 * composes both realities:
 *
 * ```
 * effective threshold = ceil(base threshold × difficulty multiplier)
 * ```
 *
 * The base threshold is a per-executant parameter (metabolism, genetics, body
 * condition), configurable within [MIN_BASE_THRESHOLD]..[MAX_BASE_THRESHOLD].
 * The multiplier belongs to the exercise. With the default base of
 * [DEFAULT_BASE_THRESHOLD]:
 *
 * | Difficulty | Multiplier | Sessions |
 * |------------|------------|----------|
 * | LOW        | ×1         | 5        |
 * | MEDIUM     | ×1.5       | 8        |
 * | HIGH       | ×2         | 10       |
 */
object PlateauThresholdRule {

    const val DEFAULT_BASE_THRESHOLD = 5
    const val MIN_BASE_THRESHOLD = 3
    const val MAX_BASE_THRESHOLD = 15

    /**
     * Effective plateau threshold of an exercise, in consecutive sessions.
     *
     * [baseThreshold] is coerced into the valid range before composing: a value
     * persisted outside the range is a corrupted state, not a reason to produce a
     * nonsensical threshold.
     */
    fun effectiveThreshold(baseThreshold: Int, difficulty: ProgressionDifficulty): Int {
        val base = coerceBaseThreshold(baseThreshold)
        return ceil(base * difficulty.thresholdMultiplier).toInt()
    }

    fun isValidBaseThreshold(value: Int): Boolean =
        value in MIN_BASE_THRESHOLD..MAX_BASE_THRESHOLD

    fun coerceBaseThreshold(value: Int): Int =
        value.coerceIn(MIN_BASE_THRESHOLD, MAX_BASE_THRESHOLD)
}
