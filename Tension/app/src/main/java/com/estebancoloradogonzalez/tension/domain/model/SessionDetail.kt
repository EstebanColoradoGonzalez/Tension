package com.estebancoloradogonzalez.tension.domain.model

data class SessionDetail(
    val sessionId: Long,
    val date: String,
    val routineName: String,
    val versionNumber: Int,
    val status: String,
    val totalTonnageKg: Double,
    val totalExercises: Int,
    val completedExercises: Int,
    val exercises: List<SessionDetailExercise>,
    /**
     * Ejercicios que el plan trajo y que el ejecutante retiró de aquella sesión sin
     * reponerlos (CA-43.08). El historial registra lo ejecutado, y no haber ejecutado algo
     * que estaba previsto también es información.
     */
    val withdrawnExerciseNames: List<String> = emptyList(),
)
