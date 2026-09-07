package com.estebancoloradogonzalez.tension.ui.history

import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.ui.components.TrendPoint

sealed interface ExerciseHistoryUiState {
    data object Loading : ExerciseHistoryUiState
    data object Empty : ExerciseHistoryUiState

    /**
     * [data] llega ya filtrado al implemento seleccionado: la lista y la gráfica se
     * construyen sobre las mismas entradas, y CA-39.08 exige que una serie de polea nunca
     * comparta serie temporal con una de mancuerna.
     *
     * [equipmentOptions] son los implementos con los que el ejercicio se ha entrenado de
     * verdad. Con uno solo la pantalla lo presenta como etiqueta y no como selector: no
     * hay nada que elegir.
     */
    data class Loaded(
        val data: ExerciseHistoryData,
        val trendPoints: List<TrendPoint>,
        val yAxisLabel: String,
        val equipmentOptions: List<String>,
        val selectedEquipment: String?,
    ) : ExerciseHistoryUiState
}
