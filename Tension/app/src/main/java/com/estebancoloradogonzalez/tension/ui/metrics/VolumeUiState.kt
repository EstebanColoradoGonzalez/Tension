package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTonnage
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot

sealed interface VolumeUiState {
    data object Loading : VolumeUiState
    data class Content(
        val tonnageByGroup: List<MuscleGroupTonnage>,
        val distributionByModule: Map<String, Map<String, Double>>,
        val evolution: List<TonnageSnapshot>,
        val selectedMicrocycle: Int,
        val totalMicrocycles: Int,
        val insufficientEvolution: Boolean,
    ) : VolumeUiState
    data class Error(val message: String) : VolumeUiState
}
