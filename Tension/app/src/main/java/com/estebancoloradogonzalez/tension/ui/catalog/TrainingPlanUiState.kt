package com.estebancoloradogonzalez.tension.ui.catalog

data class TrainingPlanUiState(
    val isLoading: Boolean = true,
    val modules: List<ModuleSectionItem> = emptyList(),
)

data class ModuleSectionItem(
    val moduleCode: String,
    val moduleName: String,
    val groupDescription: String,
    val versions: List<VersionItem>,
)

data class VersionItem(
    val moduleVersionId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
