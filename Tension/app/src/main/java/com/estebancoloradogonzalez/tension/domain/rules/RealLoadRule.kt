package com.estebancoloradogonzalez.tension.domain.rules

/**
 * The real load of a set: the external load the executant typed in plus the share of their
 * own body that the exercise moves (HU-49, Rule 4).
 *
 * `real load = external load + body weight in force on the session date × percentage / 100`
 *
 * **It is not part of the decision engine.** Like [OneRmRule] and [BodyWeightRule], it lives
 * here because it is pure and testable on the JVM. It feeds the tonnage — in every view and in
 * the tonnage-drop alert — and the estimated 1RM, and nothing else: no load prescription,
 * Double Threshold, plateau, regression, fatigue or deload rule reads it (CA-49.09). The set
 * itself keeps storing the external load; the real load is always computed, never persisted.
 *
 * The body weight only applies when the three conditions hold (CA-49.07):
 *
 * - the exercise declares a percentage greater than zero;
 * - the exercise is not isometric, because its «repetitions» are seconds held;
 * - the implement adds body weight ([addsBodyWeight]).
 *
 * In any other case the real load **is** the external load, exactly as before this story.
 */
object RealLoadRule {

    const val MIN_PERCENT = 0
    const val MAX_PERCENT = 100

    /**
     * Implements that add body weight: free weights and the executant's own body (Rule 3).
     *
     * An **allow-list**, not a deny-list, compared against the seed catalog name like
     * [ExternalLoadRule] does. An implement that is not here — `Máquina`, `Polea`,
     * `Banda Elástica`, `TRX/Suspensión` or any unknown name — does not add body weight, which
     * leaves its tonnage exactly as it was: the machine guides or supports the body, the band's
     * resistance is not a weight, and on a suspension trainer the body is assisted at a varying
     * angle. That includes the assisted pull-up on `Máquina`, whose counterweight subtracts
     * effort.
     */
    private val IMPLEMENTS_ADDING_BODY_WEIGHT = setOf(
        ExternalLoadRule.PESO_CORPORAL,
        "Barra Fija",
        ExternalLoadRule.PESO_ANADIDO,
        "Mancuerna",
        "Barra",
        "Barra EZ",
        "Máquina Smith",
        "Pesa Rusa",
        "Balón Medicinal",
        "Paralelas/Dip Station",
        "Rodillo de Abdomen",
    )

    fun addsBodyWeight(equipmentTypeName: String?): Boolean =
        equipmentTypeName in IMPLEMENTS_ADDING_BODY_WEIGHT

    /** Whether the set adds the executant's body weight to its external load (Rule 4). */
    fun appliesBodyWeight(
        bodyWeightPercent: Int,
        isIsometric: Boolean,
        equipmentTypeName: String?,
    ): Boolean = bodyWeightPercent > 0 && !isIsometric && addsBodyWeight(equipmentTypeName)

    /**
     * The real load of one set, in kilograms.
     *
     * [bodyWeightKg] is the body weight in force on the session date — the latest entry of the
     * history dated on or before it (CA-49.06) — or null when the history has none that early.
     * Without a body weight there is nothing to add, and the set counts its external load:
     * inventing a weight would put a number in the tonnage that the executant never recorded.
     */
    fun realLoadKg(
        externalLoadKg: Double,
        bodyWeightKg: Double?,
        bodyWeightPercent: Int,
        isIsometric: Boolean,
        equipmentTypeName: String?,
    ): Double {
        if (bodyWeightKg == null) return externalLoadKg
        if (!appliesBodyWeight(bodyWeightPercent, isIsometric, equipmentTypeName)) return externalLoadKg
        return externalLoadKg + bodyWeightKg * bodyWeightPercent / 100.0
    }

    /**
     * Parses the percentage typed in the exercise detail (CA-49.05): an integer from 0 to 100,
     * both included. A decimal, a sign, a blank or any other text yields null, and so does a
     * value out of range — the caller rejects all of them the same way.
     */
    fun parsePercent(input: String): Int? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return null
        val value = trimmed.toIntOrNull() ?: return null
        return value.takeIf { isValidPercent(it) }
    }

    fun isValidPercent(value: Int): Boolean = value in MIN_PERCENT..MAX_PERCENT
}
