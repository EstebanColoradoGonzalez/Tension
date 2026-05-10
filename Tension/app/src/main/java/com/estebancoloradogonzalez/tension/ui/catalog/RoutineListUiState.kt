package com.estebancoloradogonzalez.tension.ui.catalog

data class RoutineListUiState(
    val isLoading: Boolean = true,
    val routines: List<RoutineItem> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createDialogName: String = "",
    val showEditDialog: Boolean = false,
    val editTarget: RoutineItem? = null,
    val editDialogName: String = "",
    val deleteTarget: RoutineItem? = null,
    val errorMessage: String? = null,
)

data class RoutineItem(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val versionCount: Int,
)
