package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.CorrectableSet
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * The set the correction form preloads (CA-47.03), or `null` if [sessionExerciseId] does not
 * own the correction window. The window is resolved by the repository on the data itself, so
 * it is the same answer after an unexpected shutdown (CA-47.10).
 */
class GetCorrectableSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionExerciseId: Long): CorrectableSet? {
        return sessionRepository.getCorrectableSet(sessionExerciseId)
    }
}
