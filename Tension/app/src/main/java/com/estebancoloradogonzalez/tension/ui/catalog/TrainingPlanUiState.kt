package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.WeekDay

data class TrainingPlanUiState(
    val isLoading: Boolean = true,
    val routines: List<RoutineSectionItem> = emptyList(),
    /** Días sin rutina asignada, presentados como descanso al final de la lista. */
    val restDays: List<WeekDay> = emptyList(),
)

data class RoutineSectionItem(
    val routineId: Long,
    val routineName: String,
    /** Días que ejecutan esta rutina. Vacío cuando ninguno la reclama. */
    val weekDays: List<WeekDay>,
    val versions: List<VersionItem>,
    /** Categoría de la rutina (HU-46, CA-46.09). */
    val movementCategory: MovementCategory? = null,
)

data class VersionItem(
    val routineVersionId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
