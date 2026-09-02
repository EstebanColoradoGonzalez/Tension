package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetTrainingPlanUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetWeekDayPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrainingPlanViewModel @Inject constructor(
    getTrainingPlanUseCase: GetTrainingPlanUseCase,
    getWeekDayPlanUseCase: GetWeekDayPlanUseCase,
) : ViewModel() {

    val uiState: StateFlow<TrainingPlanUiState> =
        combine(
            getTrainingPlanUseCase(),
            getWeekDayPlanUseCase(),
        ) { routinesWithVersions, weekDayPlan ->
            val daysByRoutineId = weekDayPlan
                .mapNotNull { day -> day.routineId?.let { it to day.weekDay } }
                .groupBy({ it.first }, { it.second })

            TrainingPlanUiState(
                isLoading = false,
                routines = routinesWithVersions.map { rwv ->
                    RoutineSectionItem(
                        routineId = rwv.routine.id,
                        routineName = rwv.routine.name,
                        weekDays = daysByRoutineId[rwv.routine.id].orEmpty(),
                        versions = rwv.versions.map { vs ->
                            VersionItem(
                                routineVersionId = vs.routineVersionId,
                                versionNumber = vs.versionNumber,
                                exerciseCount = vs.exerciseCount,
                            )
                        },
                    )
                },
                restDays = weekDayPlan.filter { it.isRestDay }.map { it.weekDay },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrainingPlanUiState(),
        )
}
