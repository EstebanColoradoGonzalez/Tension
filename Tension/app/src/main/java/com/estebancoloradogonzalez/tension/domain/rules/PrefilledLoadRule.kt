package com.estebancoloradogonzalez.tension.domain.rules

object PrefilledLoadRule {

    private const val WEIGHT_TOLERANCE = 0.01

    /**
     * Resolves the load prefilled on the set registration screen, applying a strict
     * precedence:
     *
     * 1. The load prescribed by the double threshold engine, while it is still *active*
     * 2. The weight of the previous set of the same exercise in the current session
     * 3. The weight of the last set of the same exercise in its previous session
     * 4. No value — the field stays empty
     *
     * A prescription is *active* while it exceeds the last weight actually handled: it
     * represents an increase the executant has not reached yet. Once reached or surpassed,
     * the prescription is spent and the memory of the last handled weight takes over, so
     * the field follows the progression instead of falling back to a frozen value.
     *
     * @return the load in kilograms, or null when there is nothing to prefill.
     */
    fun resolve(
        prescribedLoadKg: Double?,
        lastWeightInSessionKg: Double?,
        lastWeightInPreviousSessionKg: Double?,
    ): Double? {
        val memoryKg = lastWeightInSessionKg ?: lastWeightInPreviousSessionKg
        val prescribedKg = prescribedLoadKg?.takeIf { it > 0.0 }

        val isPrescriptionActive = prescribedKg != null &&
            (memoryKg == null || prescribedKg - memoryKg > WEIGHT_TOLERANCE)

        return if (isPrescriptionActive) prescribedKg else memoryKg
    }
}
