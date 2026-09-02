package com.estebancoloradogonzalez.tension.domain.model

/**
 * Intrinsic progression capacity of an exercise. Isolation work on small muscle
 * zones advances far slower than heavy multi-joint work, partly because the
 * smallest available increment represents a much larger share of the working load.
 *
 * [thresholdMultiplier] scales the executant's base plateau threshold: the effective
 * threshold of an exercise is the base threshold multiplied by this factor, rounded up.
 */
enum class ProgressionDifficulty(val thresholdMultiplier: Double) {
    LOW(1.0),
    MEDIUM(1.5),
    HIGH(2.0),
    ;

    companion object {
        /**
         * Resolves a persisted code into a difficulty. Every exercise has a difficulty:
         * an unknown or absent code falls back to [MEDIUM], never to an undefined state.
         */
        fun fromCode(code: String?): ProgressionDifficulty {
            return entries.firstOrNull { it.name == code } ?: MEDIUM
        }
    }
}
