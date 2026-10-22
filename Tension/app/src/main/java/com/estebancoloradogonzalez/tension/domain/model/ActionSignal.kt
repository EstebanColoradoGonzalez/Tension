package com.estebancoloradogonzalez.tension.domain.model

sealed interface ActionSignal {
    /**
     * Raise the load to [targetKg]. Since HU-46 it is earned by [streak] consecutive
     * top-of-range sessions with the same weight; `null` keeps the legacy reading.
     */
    data class IncreaseLoad(val targetKg: Double, val streak: Int? = null) : ActionSignal
    data object ProgressInReps : ActionSignal
    data class MaintainLoad(val message: String) : ActionSignal
    data object ConsiderDeload : ActionSignal
    data class BodyweightSignal(val totalReps: Int, val diff: Int?) : ActionSignal
    data class IsometricSignal(val setCount: Int, val avgSeconds: Int) : ActionSignal
    data object IsometricMastered : ActionSignal
    data object FirstSession : ActionSignal
    data object DeloadSession : ActionSignal

    /** Below the 8–12 range: lower the weight, by the amount the executant chooses (CA-46.02). */
    data object DecreaseLoad : ActionSignal

    /** Bodyweight pair that admits «Peso Añadido», after [streak] top sessions (CA-46.15). */
    data class AddExtraWeight(val streak: Int) : ActionSignal

    /** At the top of the range, [streak] of the sessions needed to increase (CA-46.04). */
    data class TopOfRange(val streak: Int) : ActionSignal

    /** Inside the 8–12 range: keep the weight. */
    data object WithinRange : ActionSignal
}
