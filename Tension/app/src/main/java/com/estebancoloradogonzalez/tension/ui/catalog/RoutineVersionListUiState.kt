package com.estebancoloradogonzalez.tension.ui.catalog

data class RoutineVersionListUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val versions: List<RoutineVersionItem> = emptyList(),
    val deleteTarget: RoutineVersionItem? = null,
    val errorMessage: String? = null,
)

data class RoutineVersionItem(
    val id: Long,
    val routineId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
