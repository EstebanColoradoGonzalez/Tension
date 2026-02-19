package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class GetSessionHistoryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): List<SessionHistoryItem> =
        sessionRepository.getSessionHistory()
}
