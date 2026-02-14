package com.estebancoloradogonzalez.tension.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetWeightHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightHistoryViewModel @Inject constructor(
    private val getWeightHistoryUseCase: GetWeightHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightHistoryUiState())
    val uiState: StateFlow<WeightHistoryUiState> = _uiState.asStateFlow()

    init {
        loadWeightHistory()
    }

    private fun loadWeightHistory() {
        viewModelScope.launch {
            getWeightHistoryUseCase().collect { records ->
                val oldestDate = records.minByOrNull { it.date }?.date
                val entries = records.map { record ->
                    WeightEntryItem(
                        weightKg = record.weightKg,
                        date = record.date,
                        isInitialRecord = record.date == oldestDate,
                    )
                }
                _uiState.value = WeightHistoryUiState(
                    isLoading = false,
                    weightEntries = entries,
                )
            }
        }
    }
}
