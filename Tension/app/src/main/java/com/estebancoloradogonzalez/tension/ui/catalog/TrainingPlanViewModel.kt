package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetTrainingPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrainingPlanViewModel @Inject constructor(
    getTrainingPlanUseCase: GetTrainingPlanUseCase,
) : ViewModel() {

    val uiState: StateFlow<TrainingPlanUiState> =
        getTrainingPlanUseCase().map { modulesWithVersions ->
            TrainingPlanUiState(
                isLoading = false,
                modules = modulesWithVersions.map { mwv ->
                    ModuleSectionItem(
                        moduleCode = mwv.module.code,
                        moduleName = mwv.module.name,
                        groupDescription = mwv.module.groupDescription,
                        versions = mwv.versions.map { vs ->
                            VersionItem(
                                moduleVersionId = vs.moduleVersionId,
                                versionNumber = vs.versionNumber,
                                exerciseCount = vs.exerciseCount,
                            )
                        },
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrainingPlanUiState(),
        )
}
