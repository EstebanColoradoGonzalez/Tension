package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification

/**
 * Consolidates the state of the (exercise, equipment) pairs into the reading of the
 * exercise (HU-40).
 *
 * Isolating the pairs is what removes the false regression, but isolating them and stopping
 * there would have a cost: alternating two implements, each history would advance at half
 * speed and a plateau would take twice as long to surface. Two complementary boolean rules
 * pay that cost back:
 *
 * - **Progression is a disjunction.** The exercise progresses if *any* of its pairs
 *   progressed. This is what stops alternating implements from diluting overall progression.
 *   [consolidate] is where that disjunction lives: it is applied to the classifications of
 *   one session, because that is the form in which every consumer of the consolidated
 *   reading — the progression rate, the comparative KPIs, the `LOW_PROGRESSION_RATE` and
 *   `PLATEAU` alerts — already reads it.
 * - **Plateau is a conjunction.** The exercise plateaus only if *all* of its pairs are
 *   stalled ([isInPlateau]). This is what stops a plateau being declared that one implement
 *   contradicts.
 *
 * Nothing here is persisted as a state: the consolidated reading is derived from the pairs
 * on every evaluation, so there is no second copy of their state that could drift out of
 * sync with them.
 */
object ProgressionConsolidationRule {

    /**
     * CA-40.05 — the exercise plateaus only when *every* pair has reached the effective
     * threshold of the exercise.
     *
     * An exercise with no pair at all is not in a plateau: an empty conjunction is
     * vacuously true, and answering "yes" for something never trained would raise an alert
     * about nothing.
     */
    fun isInPlateau(pairCounters: List<Int>, effectiveThreshold: Int): Boolean {
        if (pairCounters.isEmpty()) return false
        return pairCounters.all { it >= effectiveThreshold }
    }

    /**
     * CA-40.02 / CA-40.04 — the classification of the exercise in one session, consolidated
     * from the classification of each pair trained in it.
     *
     * CA-40.04 states the consolidation in boolean terms (progresses / does not) while the
     * classification has three values and a null. The disjunction is preserved by keeping
     * the *best* classified pair, ordering
     * `POSITIVE_PROGRESSION > MAINTENANCE > REGRESSION`:
     *
     * - any pair progressed → the exercise progressed;
     * - `REGRESSION` only survives when every classified pair regressed;
     * - unclassified pairs are ignored, and when *all* of them are unclassified the result
     *   is null — which is precisely the case of breaking in a new implement: no history to
     *   compare against is not a regression.
     */
    fun consolidate(
        pairClassifications: List<ProgressionClassification?>,
    ): ProgressionClassification? {
        val classified = pairClassifications.filterNotNull()
        if (classified.isEmpty()) return null

        return when {
            classified.contains(ProgressionClassification.POSITIVE_PROGRESSION) ->
                ProgressionClassification.POSITIVE_PROGRESSION

            classified.contains(ProgressionClassification.MAINTENANCE) ->
                ProgressionClassification.MAINTENANCE

            else -> ProgressionClassification.REGRESSION
        }
    }
}
