package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.SessionPreviewExercise
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionPreviewUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(moduleVersionId: Long): Flow<List<SessionPreviewExercise>> {
        return sessionRepository.getSessionPreviewExercises(moduleVersionId)
    }
}
