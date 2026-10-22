package com.estebancoloradogonzalez.tension.domain.model

/**
 * Movement category of an exercise and of a routine: the Push / Pull / Lower split that gives
 * the plan its meaning (HU-46).
 *
 * Until this story the split only existed in the routine's free-text name, so nothing could
 * validate against it. Now it is a datum of both the exercise and the routine, and the
 * routine's category is the **category of the day**: the session and the plan only offer
 * exercises of that category.
 *
 * Every exercise and every routine has exactly one. Anything that trains the abdomen is
 * [PULL] (RN-10), and [LEGS] is fully isolated from the other two. The Spanish labels —
 * "Empuje", "Jalón", "Pierna" — are presentation and live in `strings.xml`, not here.
 */
enum class MovementCategory {
    PUSH,
    PULL,
    LEGS,
    ;

    companion object {
        /**
         * Resolves a persisted code. Unlike `ProgressionDifficulty`, an unknown code yields
         * `null` instead of a fallback: inventing a category for a corrupted value would place
         * an exercise in a split nobody chose.
         */
        fun fromCode(code: String?): MovementCategory? = entries.firstOrNull { it.name == code }
    }
}
