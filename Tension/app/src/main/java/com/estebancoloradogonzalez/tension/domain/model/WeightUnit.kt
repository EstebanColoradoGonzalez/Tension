package com.estebancoloradogonzalez.tension.domain.model

/**
 * Capture unit for exercise load. Kilogram is the canonical unit of the system:
 * this enum only expresses how the executant typed the value in, never how it is
 * stored, aggregated or compared.
 *
 * [captureStep] is the amount a single tap on the increase/decrease control applies,
 * expressed in the unit itself.
 */
enum class WeightUnit(val captureStep: Double) {
    KG(0.5),
    LB(1.0),
    ;

    companion object {
        fun fromCode(code: String?): WeightUnit {
            return entries.firstOrNull { it.name == code } ?: KG
        }
    }
}
