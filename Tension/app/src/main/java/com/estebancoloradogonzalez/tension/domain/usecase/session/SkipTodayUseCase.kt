package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Declara que hoy no se entrena. Es la única forma de cancelar el día.
 *
 * Resuelve el día **sin dejar sesión alguna**: si había una en curso sin series, se descarta.
 * Omitir un día es exactamente no haber entrenado, así que no aparece en el historial, no
 * cuenta como adherencia y no silencia la alerta de inactividad.
 *
 * Se rechaza en cuanto hay **una sola serie registrada**: a partir de ahí sí hubo
 * entrenamiento, y lo ocurrido se conserva cerrando la sesión como incompleta. Cancelarlo
 * borraría trabajo real.
 */
class SkipTodayUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        if (sessionRepository.hasSetsInActiveSession()) {
            throw IllegalStateException("Cannot skip the day: the session already has sets")
        }
        sessionRepository.skipToday()
    }
}
