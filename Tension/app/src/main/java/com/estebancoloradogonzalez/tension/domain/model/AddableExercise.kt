package com.estebancoloradogonzalez.tension.domain.model

/**
 * Ejercicio del Diccionario, visto desde la hoja de añadir de la sesión activa (HU-43).
 *
 * Los que ya están en la sesión **viajan igual**, marcados: CA-43.01 no los esconde, los
 * hace no seleccionables. Ocultarlos dejaría al ejecutante buscando algo que sí existe sin
 * decirle por qué no aparece, y la razón —entrenar el mismo movimiento con otro implemento
 * se resuelve con el selector de equipamiento— solo puede leerse si el ejercicio se ve.
 */
data class AddableExercise(
    val exerciseId: Long,
    val name: String,
    val equipmentSummary: String,
    val muscleZonesSummary: String,
    val isAlreadyInSession: Boolean,
)
