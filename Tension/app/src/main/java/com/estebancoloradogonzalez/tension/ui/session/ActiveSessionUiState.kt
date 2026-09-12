package com.estebancoloradogonzalez.tension.ui.session

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus

data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val versionNumber: Int = 0,
    val exercises: List<ExerciseUiItem> = emptyList(),
    val showCloseDialog: Boolean = false,
    val isClosing: Boolean = false,
    val isDeloadSession: Boolean = false,
    val deloadProgress: String = "",
    val errorMessage: String? = null,
    /** Cuantos ejercicios ha anadido el ejecutante a esta sesion (CA-43.04). */
    val addedCount: Int = 0,
    /** Cuantos del plan ha retirado. Nunca por encima de [addedCount] (CA-43.05). */
    val withdrawnCount: Int = 0,
) {
    val completedCount: Int get() = exercises.count { it.status == ExerciseSessionStatus.COMPLETED }
    val totalCount: Int get() = exercises.size
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val incompleteCount: Int get() = totalCount - completedCount
    val isAllCompleted: Boolean get() = completedCount == totalCount && totalCount > 0

    /** Si no hay ninguna serie, cerrar descarta la sesión en lugar de guardarla. */
    val hasAnySetRegistered: Boolean get() = exercises.any { it.completedSets > 0 }

    /** Cuantos del plan pueden retirarse todavia: uno entra, uno puede salir (CA-43.04). */
    val withdrawalBudget: Int get() = addedCount - withdrawnCount
}

data class AlternativeSelectionUiState(
    val isVisible: Boolean = false,
    val sessionExerciseId: Long = 0,
    val slot: Int = 0,
    val alternatives: List<AlternativeOption> = emptyList(),
    val selectedExerciseId: Long? = null,
)

data class AlternativeOption(
    val exerciseId: Long,
    val name: String,
    val equipmentSummary: String,
    val muscleZonesSummary: String,
)

data class ExerciseUiItem(
    val sessionExerciseId: Long,
    val exerciseId: Long?,
    val name: String,
    val equipmentSummary: String,
    val muscleZones: String,
    val sets: Int,
    val reps: String,
    val prescribedLoadKg: Double?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val completedSets: Int,
    val status: ExerciseSessionStatus,
    val loadDisplayText: String,
    val statusDisplayText: String,
    val isFinalized: Boolean,
    val slot: Int,
    val hasAlternatives: Boolean,
    /** Anadido a esta sesion: se marca, no ocupa puesto y no tiene alternativas (CA-43.01). */
    val isExtra: Boolean = false,
    /**
     * Por que no puede retirarse, ya redactado. `null` habilita la accion.
     *
     * El mensaje se resuelve en el ViewModel a partir del veredicto de
     * `SessionAdjustmentRule`, y no en el Composable, porque la causa depende de datos que
     * la fila no tiene —cuantos anadidos hay y que ejercicio esta pendiente de reponer—.
     */
    val withdrawalBlockReason: String? = null,
)

/** Hoja de seleccion para anadir un ejercicio a la sesion (CA-43.01, CA-43.02). */
data class AddExerciseSheetState(
    val isVisible: Boolean = false,
    val query: String = "",
    val exercises: List<AddableExerciseUiItem> = emptyList(),
    val isAdding: Boolean = false,
) {
    /** Filtrado en memoria: el catalogo completo ya viaja en el estado. */
    val visibleExercises: List<AddableExerciseUiItem>
        get() = if (query.isBlank()) {
            exercises
        } else {
            exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
}

data class AddableExerciseUiItem(
    val exerciseId: Long,
    val name: String,
    val equipmentSummary: String,
    val muscleZonesSummary: String,
    /** Ya esta en la sesion: se muestra, atenuado, y no se puede elegir (CA-43.01). */
    val isAlreadyInSession: Boolean,
)

/** Confirmacion del retiro, que enuncia que el plan no cambia (CA-43.04). */
data class WithdrawDialogState(
    val isVisible: Boolean = false,
    val sessionExerciseId: Long = 0,
    val exerciseName: String = "",
    val isExtra: Boolean = false,
)
