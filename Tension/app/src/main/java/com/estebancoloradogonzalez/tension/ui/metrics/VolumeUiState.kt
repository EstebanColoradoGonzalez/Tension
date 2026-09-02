package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTonnage
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot

sealed interface VolumeUiState {
    data object Loading : VolumeUiState

    /**
     * [sessionsInSelectedMicrocycle] is what separates a muscle group that was simply
     * not trained inside a microcycle with sessions — a legitimate zero — from a
     * microcycle with no session at all, where no tonnage can be computed.
     */
    data class Content(
        val tonnageByGroup: List<MuscleGroupTonnage>,
        val distributionByMuscleGroup: Map<String, Map<String, Double>>,
        val evolution: List<TonnageSnapshot>,
        val selectedMicrocycle: Int,
        val totalMicrocycles: Int,
        val sessionsInSelectedMicrocycle: Int,
        val insufficientEvolution: Boolean,
    ) : VolumeUiState

    data class Error(val message: String) : VolumeUiState
}
