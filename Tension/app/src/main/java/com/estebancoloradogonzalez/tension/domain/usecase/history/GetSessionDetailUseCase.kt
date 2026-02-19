package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class GetSessionDetailUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): SessionDetail =
        sessionRepository.getSessionDetail(sessionId)
}
