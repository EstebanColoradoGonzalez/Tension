package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.WeekDay

data class RoutineListUiState(
    val isLoading: Boolean = true,
    val routines: List<RoutineItem> = emptyList(),
    /** Días que ninguna rutina reclama. Se presentan como descanso al cierre de la lista. */
    val restDays: List<WeekDay> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createDialogName: String = "",
    val showEditDialog: Boolean = false,
    val editTarget: RoutineItem? = null,
    val editDialogName: String = "",
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
)
