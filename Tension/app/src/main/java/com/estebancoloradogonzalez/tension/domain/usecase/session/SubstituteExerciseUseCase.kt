package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class SubstituteExerciseUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionExerciseId: Long, newExerciseId: Long) {
        sessionRepository.substituteExercise(sessionExerciseId, newExerciseId)
    }
}
