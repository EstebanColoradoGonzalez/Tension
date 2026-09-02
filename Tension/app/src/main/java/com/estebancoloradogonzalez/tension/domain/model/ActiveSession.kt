package com.estebancoloradogonzalez.tension.domain.model

data class ActiveSession(
    val sessionId: Long,
    val routineName: String,
    val versionNumber: Int,
    val totalExercises: Int,
    val completedExercises: Int,
    val registeredSets: Int = 0,
) {
    /**
     * Sin ninguna serie, la sesión todavía puede cancelarse: no hay nada que registrar y
     * cerrarla no tendría qué guardar. Con al menos una, la única salida es cerrarla
     * formalmente — como completa o como incompleta.
     */
    val canBeCancelled: Boolean get() = registeredSets == 0
}
