package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class GetExerciseHistoryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(exerciseId: Long): ExerciseHistoryData =
        sessionRepository.getExerciseHistory(exerciseId)
}
