package com.estebancoloradogonzalez.tension.ui.profile

import java.time.LocalDate

data class WeightHistoryUiState(
    val isLoading: Boolean = true,
    val weightEntries: List<WeightEntryItem> = emptyList(),
)

data class WeightEntryItem(
    val weightKg: Double,
    val date: LocalDate,
    val isInitialRecord: Boolean,
)
