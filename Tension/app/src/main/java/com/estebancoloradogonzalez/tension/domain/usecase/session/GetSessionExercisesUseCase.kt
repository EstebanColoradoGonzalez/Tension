package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionExercisesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(sessionId: Long): Flow<List<SessionExerciseDetail>> {
        return sessionRepository.getSessionExercises(sessionId)
    }
}
