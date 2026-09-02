package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import javax.inject.Inject

/**
 * Cierra la sesión en curso.
 *
 * Requiere al menos una serie registrada: una sesión vacía no se cierra, se cancela desde
 * Inicio con "Hoy no entreno".
 */
class CloseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val recalculateTreeStateUseCase: RecalculateTreeStateUseCase,
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.closeSession(sessionId)

        // El árbol reacciona de inmediato al entrenamiento recién registrado. Es best-effort:
        // un árbol desactualizado es un defecto visual que el siguiente cambio de día corrige,
        // mientras que dejar propagar la excepción convertiría una sesión ya cerrada en un
        // error para el ejecutante.
        try {
            recalculateTreeStateUseCase()
        } catch (_: Exception) {
            // El árbol no puede hacer fallar el cierre de una sesión.
        }
    }
}
