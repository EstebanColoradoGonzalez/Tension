package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Resuelve la sesión que quedó abierta de un día anterior.
 *
 * Pasada la medianoche, una sesión en curso ya no puede continuarse: pertenece a un día que
 * terminó. El sistema hace por el ejecutante lo que él habría hecho manualmente:
 *
 * - **Con al menos una serie registrada** se cierra con el protocolo completo — clasificación
 *   de progresión, avance de rotación, alertas — y queda `INCOMPLETE`, exactamente como si la
 *   hubiera cerrado él. La sesión conserva la fecha en que se inició, que es el día que se
 *   entrenó.
 * - **Sin ninguna serie** se descarta: se abrió y no se entrenó nada. No aparece en el
 *   historial, no cuenta como adherencia y la rotación no avanza.
 *
 * El día no entrenado no deja registro propio: su ausencia de sesión ya lo dice, y es lo que
 * leen el historial, la adherencia y la alerta de inactividad.
 */
class ResolveStaleSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        val staleSessionId = sessionRepository.getStaleActiveSessionId() ?: return
        if (sessionRepository.hasSetsInActiveSession()) {
            sessionRepository.closeSession(staleSessionId)
        } else {
            sessionRepository.discardSession(staleSessionId)
        }
    }
}
