package com.estebancoloradogonzalez.tension.ui.catalog

data class TrainingPlanUiState(
    val isLoading: Boolean = true,
    val routines: List<RoutineSectionItem> = emptyList(),
)

data class RoutineSectionItem(
    val routineId: Long,
    val routineName: String,
    val versions: List<VersionItem>,
)

data class VersionItem(
    val routineVersionId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
