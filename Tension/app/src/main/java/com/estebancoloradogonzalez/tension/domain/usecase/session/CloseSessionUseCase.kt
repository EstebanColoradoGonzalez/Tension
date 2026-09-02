package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Cierra la sesión en curso.
 *
 * Requiere al menos una serie registrada: una sesión vacía no se cierra, se cancela desde
 * Inicio con "Hoy no entreno".
 */
class CloseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.closeSession(sessionId)
    }
}
