package com.estebancoloradogonzalez.tension.domain.rules

import kotlin.math.abs

/** How a session of one (exercise, implement) pair reads against the 8–12 range (CA-46.01). */
enum class RangeReading {
    /** The strict majority of the sets reached 12 or more reps. */
    TOP,

    /** Neither majority: the weight is right. */
    WITHIN,

    /** The strict majority of the sets stayed under 8 reps: the weight is too high. */
    BELOW,
    ;

    companion object {
        fun fromCode(code: String?): RangeReading? = entries.firstOrNull { it.name == code }
    }
}

/**
 * Consecutive top-of-range sessions of a pair **with the same reference weight**, and that
 * weight. `count = 0` means there is no streak, and then the weight is irrelevant.
 */
data class TopRangeStreak(
    val count: Int,
    val referenceWeightKg: Double?,
) {
    companion object {
        val NONE = TopRangeStreak(count = 0, referenceWeightKg = null)
    }
}

/** What the close of a session tells the executant about the weight of one pair. */
sealed interface RangeSignal {
    /** Below the range: lower the weight. No amount is prescribed (CA-46.02). */
    data object Decrease : RangeSignal

    /** Fourth (or later) consecutive top session with the same weight (CA-46.03). */
    data class Increase(val streak: Int) : RangeSignal

    /** Same as [Increase] for a pair without external load that admits «Peso Añadido» (CA-46.15). */
    data class AddExtraWeight(val streak: Int) : RangeSignal

    /** At the top, but the adaptation is still being consolidated: «N de 4» (CA-46.04). */
    data class Progressing(val streak: Int) : RangeSignal

    /** Inside the range, or a pair with no weight signal at all. Keep the weight. */
    data object InRange : RangeSignal
}

/** Live notice right after a set is registered (CA-46.05). Never blocks nor alters the set. */
sealed interface SetRangeNotice {
    /** «Baja el peso en la próxima serie». */
    data object LowerNextSet : SetRangeNotice

    /** «Tope del rango — cuenta para la subida (sesión N de 4)». Informative, not an order. */
    data class TopOfRange(val sessionNumber: Int) : SetRangeNotice

    /** «Ya toca subir el peso»: the increase was already earned and the weight did not change. */
    data object IncreaseDue : SetRangeNotice

    /** «Ya toca añadir peso extra»: [IncreaseDue] for a bodyweight pair. */
    data object AddWeightDue : SetRangeNotice
}

/**
 * Progression by the hypertrophy range (HU-46): work with a weight that allows 8 clean reps
 * and progress up to 12, with RIR 1 as the effort reference.
 *
 * It replaces the Double Threshold rule of HU-11, which demanded **4 sets** and an average
 * RIR of 2 or more. Most slots of the default plan prescribe 3 sets, so the increase signal
 * never appeared — silently. Hence the two properties this rule keeps on purpose:
 *
 * - **The number of sets prescribed by the plan never enters.** The session is read by the
 *   strict majority of the sets actually registered, whatever their number (CA-46.06).
 * - **RIR never enters.** It is the reference shown to the executant, not a condition.
 *
 * From that reading come two signals with deliberately different speeds: **lower at once**
 * — one set under 8 for the live notice, a majority for the summary — and **increase only
 * after [SESSIONS_TO_INCREASE] consecutive top sessions with the same weight**, which is the
 * margin that consolidates the adaptation (RN-3, RN-4). The weight is never lowered unless 8
 * reps are not reached (RN-5).
 *
 * A pair without external load (`ExternalLoadRule`) never receives «lower»: there is no
 * weight to take off the body. It only receives «add extra weight», and only if the exercise
 * admits «Peso Añadido» (CA-46.15). It is decided by the load and not by the implement name
 * because the strict pull-up (`Barra Fija`) and the assisted machine carry no load either.
 *
 * Isometric exercises and deload sessions are **not** passed to this rule: the caller
 * excludes them (CA-46.16, CA-46.18), and the streak stays frozen because nothing writes it.
 */
object HypertrophyRangeRule {

    const val RANGE_MIN = 8
    const val RANGE_MAX = 12
    const val SESSIONS_TO_INCREASE = 4

    private const val WEIGHT_TOLERANCE = 0.01

    /**
     * Reads a session of one pair by the **strict majority** of its sets — more than half.
     * With no majority either way (2 at the top and 2 below) the session is [RangeReading.WITHIN].
     */
    fun read(reps: List<Int>): RangeReading {
        val total = reps.size
        if (total == 0) return RangeReading.WITHIN
        return when {
            reps.count { it >= RANGE_MAX } * 2 > total -> RangeReading.TOP
            reps.count { it < RANGE_MIN } * 2 > total -> RangeReading.BELOW
            else -> RangeReading.WITHIN
        }
    }

    /**
     * Reference weight of the session: the **highest** weight of the pair's sets, never the
     * average, which can be inexact — 60 · 60 · 62.5 is a 62.5 kg session (CA-46.03).
     */
    fun referenceWeight(weightsKg: List<Double>): Double = weightsKg.maxOrNull() ?: 0.0

    /**
     * Streak after a session (CA-46.17): one more at the top with the same reference weight,
     * back to 1 at the top with another weight, back to 0 inside or below the range.
     */
    fun nextStreak(
        previous: TopRangeStreak,
        reading: RangeReading,
        referenceWeightKg: Double,
    ): TopRangeStreak {
        if (reading != RangeReading.TOP) return TopRangeStreak.NONE
        val continues = previous.count > 0 && isSameWeight(previous.referenceWeightKg, referenceWeightKg)
        return TopRangeStreak(
            count = if (continues) previous.count + 1 else 1,
            referenceWeightKg = referenceWeightKg,
        )
    }

    /**
     * Signal of a closed session from its reading and the streak **after** it.
     *
     * A pair without external load that does not admit «Peso Añadido» has no weight signal
     * at all (CA-46.15), which is [RangeSignal.InRange]: keep doing what you do.
     */
    fun sessionSignal(
        reading: RangeReading,
        streak: TopRangeStreak,
        hasExternalLoad: Boolean,
        admitsAddedWeight: Boolean,
    ): RangeSignal {
        val canIncrease = hasExternalLoad || admitsAddedWeight
        return when {
            reading == RangeReading.BELOW && hasExternalLoad -> RangeSignal.Decrease
            !canIncrease -> RangeSignal.InRange
            streak.count >= SESSIONS_TO_INCREASE && hasExternalLoad -> RangeSignal.Increase(streak.count)
            streak.count >= SESSIONS_TO_INCREASE -> RangeSignal.AddExtraWeight(streak.count)
            streak.count > 0 -> RangeSignal.Progressing(streak.count)
            else -> RangeSignal.InRange
        }
    }

    /**
     * Same reading as [sessionSignal], over the state a pair carries into the next session —
     * its last reading and its streak — for the indication next to the proposed weight
     * (CA-46.02, CA-46.04). Null when the pair has never been read by the range.
     */
    fun carriedSignal(
        lastReading: RangeReading?,
        streak: TopRangeStreak,
        hasExternalLoad: Boolean,
        admitsAddedWeight: Boolean,
    ): RangeSignal? {
        val reading = lastReading ?: return null
        return sessionSignal(reading, streak, hasExternalLoad, admitsAddedWeight)
    }

    /**
     * Live notice for a set just registered (CA-46.05).
     *
     * [streak] is the pair's streak **before** the session in course: the session being done
     * would be number `streak + 1` if it keeps the same weight, and number 1 if not. Between 8
     * and 11 reps there is nothing to say.
     */
    fun setNotice(
        reps: Int,
        weightKg: Double,
        streak: TopRangeStreak,
        hasExternalLoad: Boolean,
        admitsAddedWeight: Boolean,
    ): SetRangeNotice? {
        if (reps < RANGE_MIN) {
            return if (hasExternalLoad) SetRangeNotice.LowerNextSet else null
        }
        if (reps < RANGE_MAX) return null
        if (!hasExternalLoad && !admitsAddedWeight) return null

        val sameWeight = streak.count > 0 && isSameWeight(streak.referenceWeightKg, weightKg)
        return when {
            sameWeight && streak.count >= SESSIONS_TO_INCREASE && hasExternalLoad ->
                SetRangeNotice.IncreaseDue
            sameWeight && streak.count >= SESSIONS_TO_INCREASE -> SetRangeNotice.AddWeightDue
            sameWeight -> SetRangeNotice.TopOfRange(sessionNumber = streak.count + 1)
            else -> SetRangeNotice.TopOfRange(sessionNumber = 1)
        }
    }

    private fun isSameWeight(a: Double?, b: Double): Boolean =
        a != null && abs(a - b) < WEIGHT_TOLERANCE
}
