package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseHistoryData(
    val exerciseName: String,
    /**
     * Lifecycle status of each implement, keyed by its name (HU-40).
     *
     * There is no status of the exercise to show here: the state belongs to the pair, and
     * the screen already has an implement selector that governs which one is displayed.
     * An implement with no history is absent from the map, not present with `NO_HISTORY`.
     */
    val progressionStatusByEquipment: Map<String, String>,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    /**
     * Implementos con los que el ejercicio se ha entrenado de verdad, en orden de
     * catálogo. Vacío si no hay historial; con un solo elemento no hay nada que elegir.
     */
    val equipmentOptions: List<String>,
    val entries: List<ExerciseHistoryEntry>,
)
