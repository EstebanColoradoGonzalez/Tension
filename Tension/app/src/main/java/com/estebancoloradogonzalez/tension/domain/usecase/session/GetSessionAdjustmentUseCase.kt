package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.SessionAdjustment
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Añadidos, retirados y retiros sin reponer de la sesión en curso (CA-43.04, CA-43.05). */
class GetSessionAdjustmentUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(sessionId: Long): Flow<SessionAdjustment> =
        sessionRepository.getSessionAdjustment(sessionId)
}
