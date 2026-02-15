package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class CloseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.closeSession(sessionId)
    }
}
