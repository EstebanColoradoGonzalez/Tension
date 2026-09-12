package com.estebancoloradogonzalez.tension.domain.rules

/**
 * Estimates the one-repetition maximum of a set, and decides whether the set is entitled to
 * produce one at all (HU-42).
 *
 * **It is not part of the decision engine.** Like [ExternalLoadRule], it lives in this
 * package because it is pure and testable on the JVM, not because anything prescribes load
 * from it. The 1RM reads from the history and nothing in the system reads from the 1RM: no
 * load prescription, no Double Threshold, no plateau detection, no deload protocol, no alert
 * and no KPI invokes this object.
 *
 * **The trigger is exact: ten repetitions with an RIR of exactly one** (CA-42.03). That is
 * not a simplification, it is the combination the estimate is reliable for, and it has two
 * happy consequences:
 *
 * - The formula only ever operates at ten repetitions, which is `weight × 1.3337` — far from
 *   its indeterminacy. The denominator vanishes at 36.97 repetitions and turns negative
 *   above it, so a set of forty repetitions would yield a negative 1RM.
 * - With the repetitions fixed, the result is monotone in the weight. The two rules the
 *   executant asked for — recalculate when the weight goes up, keep the highest value ever
 *   reached — therefore collapse into one: the 1RM is the **maximum over the qualifying
 *   sets**, and it never decreases.
 */
object OneRmRule {

    /** The reference set: exactly this many repetitions, no more and no fewer. */
    const val REFERENCE_REPS = 10

    /** The reference set: exactly this RIR. Zero and two do not qualify. */
    const val REFERENCE_RIR = 1

    private const val FORMULA_INTERCEPT = 1.0278
    private const val FORMULA_SLOPE = 0.0278

    /**
     * Whether this set produces a 1RM.
     *
     * [hasExternalLoad] is the verdict of [ExternalLoadRule] for the pair, and it is what
     * carries the whole of CA-42.05: an isometric never qualifies — its repetitions are
     * seconds held, so «ten repetitions» means nothing —, a set logged with `Peso Corporal`
     * never qualifies, and on a bodyweight exercise neither `Barra Fija` nor an assisted
     * `Máquina` do, because their load is a counterweight that subtracts effort. `Peso
     * Añadido` does qualify, and over the ballast alone: the executant's bodyweight is not
     * added.
     *
     * The [weightKg] guard is not in the criteria and is deliberate. A machine typed in at
     * zero would otherwise store a record of `0.0 kg`, and CA-42.07 is explicit that a pair
     * without a real value is not shown — neither as a zero nor as a dash. Not creating the
     * row is cheaper than creating it and hiding it afterwards.
     */
    fun qualifies(
        reps: Int,
        rir: Int,
        hasExternalLoad: Boolean,
        weightKg: Double,
    ): Boolean = hasExternalLoad &&
        weightKg > 0.0 &&
        reps == REFERENCE_REPS &&
        rir == REFERENCE_RIR

    /**
     * `1RM = weight / [1.0278 − (0.0278 × reps)]`.
     *
     * Written in full rather than pre-folded into `weight × 1.3337`, even though [qualifies]
     * guarantees ten repetitions is the only value it is ever invoked with. The magic
     * constant would hide the shape of the denominator, which is the very thing the criteria
     * wanted on the record; the equivalence is asserted in the tests instead, which is where
     * it is worth something.
     */
    fun estimate(weightKg: Double, reps: Int): Double =
        weightKg / (FORMULA_INTERCEPT - FORMULA_SLOPE * reps)
}
