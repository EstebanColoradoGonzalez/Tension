package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.OneRmCandidate
import com.estebancoloradogonzalez.tension.domain.model.OneRmRecord

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
 *   sets**, and new sets never lower it.
 *
 * Since HU-49 the weight is the **real load** of the set ([RealLoadRule]): the external load
 * plus the body weight the exercise moves. The record can therefore go down when the history
 * itself is rewritten — a corrected set (HU-47) or a changed percentage (CA-49.08) — and it
 * is always rebuilt from the reference sets by [record].
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
     * [hasCountableLoad] says whether the implement carries load the 1RM may count: external
     * load by [ExternalLoadRule], or the executant's body weight by [RealLoadRule] (HU-49). It
     * carries CA-42.05 and CA-49.07 together: an isometric never qualifies — its repetitions
     * are seconds held, so «ten repetitions» means nothing —, and an assisted pull-up on
     * `Máquina` neither captures load nor adds body weight, because its counterweight
     * subtracts effort. `Peso Corporal` and `Barra Fija` qualify once the exercise declares a
     * percentage, and `Peso Añadido` counts the body **plus** the ballast (CA-49.02).
     *
     * [loadKg] is the real load of the set. Its guard is not in the criteria and is
     * deliberate: a machine typed in at zero, or a body-weight set without a percentage, would
     * otherwise store a record of `0.0 kg`, and CA-42.07 is explicit that a pair without a real
     * value is not shown — neither as a zero nor as a dash.
     */
    fun qualifies(
        reps: Int,
        rir: Int,
        hasCountableLoad: Boolean,
        loadKg: Double,
    ): Boolean = hasCountableLoad &&
        loadKg > 0.0 &&
        reps == REFERENCE_REPS &&
        rir == REFERENCE_RIR

    /**
     * Whether a set of this pair carries load the 1RM may count: external load, or body
     * weight added by the implement (see [qualifies]).
     */
    fun hasCountableLoad(
        isBodyweight: Boolean,
        isIsometric: Boolean,
        equipmentTypeName: String?,
        bodyWeightPercent: Int,
    ): Boolean = ExternalLoadRule.isCaptureEnabled(isBodyweight, isIsometric, equipmentTypeName) ||
        RealLoadRule.appliesBodyWeight(bodyWeightPercent, isIsometric, equipmentTypeName)

    /**
     * The record of a pair from its candidate sets, with the date it was reached (HU-49,
     * CA-49.02), or null when none qualifies.
     *
     * The record is the **maximum over the qualifying sets** (CA-42.04), now over their real
     * load. On a tie the earliest date wins: the record dates from the session in which the
     * value was first reached, which is what «strictly greater» always meant at registration.
     *
     * It is the only way a record is written. Rebuilding always from the reference sets — at
     * registration, at correction (HU-47), when the percentage changes (CA-49.08) and when a
     * body weight is recorded — is what keeps the persisted value exact: the lesson of HU-42
     * is that a 1RM written wrong stays wrong.
     */
    fun record(candidates: List<OneRmCandidate>): OneRmRecord? =
        candidates
            .filter { qualifies(it.reps, it.rir, it.hasCountableLoad, it.loadKg) }
            .minWithOrNull(compareByDescending<OneRmCandidate> { it.loadKg }.thenBy { it.date })
            ?.let { best -> OneRmRecord(estimate(best.loadKg, best.reps), best.date) }

    /**
     * The relative strength: the 1RM as a multiple of the body weight in force on the date it
     * was reached (CA-49.03). Unitless — the same with kilograms or pounds — and null without a
     * body weight to divide by, which the screen shows as nothing rather than as an invented
     * figure.
     */
    fun relativeStrength(oneRmKg: Double, bodyWeightKg: Double?): Double? =
        bodyWeightKg?.takeIf { it > 0.0 }?.let { oneRmKg / it }

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
