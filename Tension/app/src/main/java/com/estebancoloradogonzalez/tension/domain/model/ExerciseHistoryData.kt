package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseHistoryData(
    val exerciseName: String,
    val progressionStatus: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    /**
     * Implementos con los que el ejercicio se ha entrenado de verdad, en orden de
     * catálogo. Vacío si no hay historial; con un solo elemento no hay nada que elegir.
     */
    val equipmentOptions: List<String>,
    val entries: List<ExerciseHistoryEntry>,
)
