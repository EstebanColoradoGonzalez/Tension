package com.estebancoloradogonzalez.tension.domain.rules

object AlertThresholdRule {

    const val PROGRESSION_ALERT_THRESHOLD = 40.0
    const val PROGRESSION_CRISIS_THRESHOLD = 20.0
    const val RIR_LOW_THRESHOLD = 1.5
    const val RIR_HIGH_THRESHOLD = 3.5
    const val RIR_SUSTAINED_SESSIONS = 2
    const val ADHERENCE_THRESHOLD = 60.0
    const val ADHERENCE_CRISIS_WEEKS = 2
    const val TONNAGE_ALERT_THRESHOLD = 10.0
    const val TONNAGE_CRISIS_THRESHOLD = 20.0
    const val INACTIVITY_ALERT_DAYS = 10L
    const val INACTIVITY_CRISIS_DAYS = 14L

    val MUSCLE_GROUPS_BY_MODULE: Map<String, List<String>> = mapOf(
        "A" to listOf("Espalda", "Bíceps", "Abdomen"),
        "B" to listOf("Pecho", "Hombro", "Tríceps"),
        "C" to listOf(
            "Cuádriceps",
            "Isquiotibiales",
            "Glúteos",
            "Aductores",
            "Abductores",
            "Gemelos",
        ),
    )

    fun isProgressionAlert(rate: Double): Boolean = rate < PROGRESSION_ALERT_THRESHOLD

    fun isProgressionCrisis(rate: Double): Boolean = rate < PROGRESSION_CRISIS_THRESHOLD

    fun isRirLow(avgRir: Double): Boolean = avgRir < RIR_LOW_THRESHOLD

    fun isRirHigh(avgRir: Double): Boolean = avgRir > RIR_HIGH_THRESHOLD

    fun isRirOutOfRange(avgRir: Double): Boolean = isRirLow(avgRir) || isRirHigh(avgRir)

    fun isAdherenceLow(percentage: Double): Boolean = percentage < ADHERENCE_THRESHOLD

    fun isTonnageAlert(dropPercentage: Double): Boolean =
        dropPercentage > TONNAGE_ALERT_THRESHOLD

    fun isTonnageCrisis(dropPercentage: Double): Boolean =
        dropPercentage > TONNAGE_CRISIS_THRESHOLD

    fun isInactivityAlert(days: Long): Boolean = days > INACTIVITY_ALERT_DAYS

    fun isInactivityCrisis(days: Long): Boolean = days > INACTIVITY_CRISIS_DAYS

    fun progressionLevel(rate: Double): String? {
        return when {
            isProgressionCrisis(rate) -> "CRISIS"
            isProgressionAlert(rate) -> "MEDIUM_ALERT"
            else -> null
        }
    }

    fun tonnageLevel(dropPercentage: Double, isDeloadSession: Boolean): String? {
        return when {
            isDeloadSession && isTonnageAlert(dropPercentage) -> "MEDIUM_ALERT"
            !isDeloadSession && isTonnageCrisis(dropPercentage) -> "CRISIS"
            !isDeloadSession && isTonnageAlert(dropPercentage) -> "MEDIUM_ALERT"
            else -> null
        }
    }

    fun inactivityLevel(days: Long): String? {
        return when {
            isInactivityCrisis(days) -> "CRISIS"
            isInactivityAlert(days) -> "MEDIUM_ALERT"
            else -> null
        }
    }
}
