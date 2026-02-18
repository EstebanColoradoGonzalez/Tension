package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMicrocycleMapUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetTonnageByMuscleGroupUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetTonnageEvolutionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetVolumeDistributionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VolumeViewModel @Inject constructor(
    private val getMicrocycleMapUseCase: GetMicrocycleMapUseCase,
    private val getTonnageByMuscleGroupUseCase: GetTonnageByMuscleGroupUseCase,
    private val getVolumeDistributionUseCase: GetVolumeDistributionUseCase,
    private val getTonnageEvolutionUseCase: GetTonnageEvolutionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VolumeUiState>(VolumeUiState.Loading)
    val uiState: StateFlow<VolumeUiState> = _uiState.asStateFlow()

    private var microcycleMap: Map<Int, List<Long>> = emptyMap()

    init {
        loadVolume()
    }

    private fun loadVolume() {
        viewModelScope.launch {
            try {
                microcycleMap = getMicrocycleMapUseCase()
                val totalMicrocycles = microcycleMap.size
                val selectedMicrocycle = totalMicrocycles.coerceAtLeast(1)
                val sessionIds = microcycleMap[selectedMicrocycle] ?: emptyList()
                val tonnage = getTonnageByMuscleGroupUseCase(sessionIds)
                val distribution = getVolumeDistributionUseCase(sessionIds)
                val evolution = getTonnageEvolutionUseCase(microcycleMap)
                _uiState.value = VolumeUiState.Content(
                    tonnageByGroup = tonnage,
                    distributionByModule = distribution,
                    evolution = evolution,
                    selectedMicrocycle = selectedMicrocycle,
                    totalMicrocycles = totalMicrocycles,
                    insufficientEvolution = totalMicrocycles < 2,
                )
            } catch (e: Exception) {
                _uiState.value = VolumeUiState.Error(
                    e.message ?: "Error al cargar volumen",
                )
            }
        }
    }

    fun selectMicrocycle(microcycleNumber: Int) {
        viewModelScope.launch {
            val sessionIds = microcycleMap[microcycleNumber] ?: emptyList()
            val tonnage = getTonnageByMuscleGroupUseCase(sessionIds)
            val distribution = getVolumeDistributionUseCase(sessionIds)
            val current = (_uiState.value as? VolumeUiState.Content) ?: return@launch
            _uiState.value = current.copy(
                tonnageByGroup = tonnage,
                distributionByModule = distribution,
                selectedMicrocycle = microcycleNumber,
            )
        }
    }
}
