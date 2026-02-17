package com.estebancoloradogonzalez.tension.domain.model

sealed interface ActionSignal {
    data class IncreaseLoad(val targetKg: Double) : ActionSignal
    data object ProgressInReps : ActionSignal
    data class MaintainLoad(val message: String) : ActionSignal
    data object ConsiderDeload : ActionSignal
    data class BodyweightSignal(val totalReps: Int, val diff: Int?) : ActionSignal
    data class IsometricSignal(val setCount: Int, val avgSeconds: Int) : ActionSignal
    data object IsometricMastered : ActionSignal
    data object FirstSession : ActionSignal
}
