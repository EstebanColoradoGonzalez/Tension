package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.WeekDay

data class RoutineListUiState(
    val isLoading: Boolean = true,
    val routines: List<RoutineItem> = emptyList(),
    /** Días que ninguna rutina reclama. Se presentan como descanso al cierre de la lista. */
    val restDays: List<WeekDay> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createDialogName: String = "",
    /** Sin preselección: elegirla es parte del dato (HU-46, CA-46.13). */
    val createDialogCategory: MovementCategory? = null,
    /** Se intentó crear sin categoría: el diálogo lo dice junto al campo. */
    val createCategoryMissing: Boolean = false,
    val showEditDialog: Boolean = false,
    val editTarget: RoutineItem? = null,
    val editDialogName: String = "",
    val editDialogCategory: MovementCategory? = null,
    /** Ejercicios asignados que impiden el cambio de categoría (CA-46.14). Vacía si ninguno. */
    val editCategoryConflicts: List<String> = emptyList(),
    val deleteTarget: RoutineItem? = null,
    val weekDaysTarget: RoutineItem? = null,
    val weekDaysSelection: Set<WeekDay> = emptySet(),
    val errorMessage: String? = null,
)

data class RoutineItem(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val versionCount: Int,
    /** Días que ejecutan esta rutina. Vacío cuando ninguno la reclama. */
    val weekDays: List<WeekDay> = emptyList(),
    /** Categoría de la rutina, independiente del nombre (HU-46, CA-46.09). */
    val movementCategory: MovementCategory? = null,
)
