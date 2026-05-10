package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSummaryItem
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionSummary
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.ActionSignalRule
import javax.inject.Inject

class GetSessionSummaryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): SessionSummary {
        val data = sessionRepository.getSessionSummaryData(sessionId)

        val items = data.exercises.map { dto ->
            val isDeload = dto.isDeload == 1
            val classification = dto.classification?.let {
                ProgressionClassification.valueOf(it)
            }

            val signal = ActionSignalRule.resolve(
                classification = classification,
                prescribedLoadKg = dto.prescribedLoadKg,
                avgWeightKg = dto.avgWeightKg,
                routineRequiresDeload = data.routineRequiresDeload,
                isBodyweight = dto.isBodyweight == 1,
                isIsometric = dto.isIsometric == 1,
                totalReps = dto.totalReps,
                previousTotalReps = dto.previousTotalReps,
                setCount = dto.setCount,
                isMastered = dto.isMastered == 1,
                isDeload = isDeload,
            )

            ExerciseSummaryItem(
                exerciseId = dto.exerciseId,
                name = dto.exerciseName,
                classification = classification,
                signal = signal,
                weightKg = dto.avgWeightKg,
                isBodyweight = dto.isBodyweight == 1,
                isIsometric = dto.isIsometric == 1,
                isMastered = dto.isMastered == 1,
                isDeload = isDeload,
                completedSets = dto.setCount,
                prescribedSets = dto.prescribedSets,
            )
        }

        return SessionSummary(
            status = data.info.status,
            routineName = data.info.routineName,
            versionNumber = data.info.versionNumber,
            totalTonnageKg = data.info.totalTonnageKg,
            completedExercises = data.info.completedExercises,
            totalExercises = data.info.totalExercises,
            exercises = items,
        )
    }
}
