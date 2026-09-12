package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Añade a la sesión en curso un ejercicio del Diccionario (CA-43.01).
 *
 * El ajuste es **temporal**: el plan por defecto no se modifica, y la siguiente sesión de
 * esa misma rutina vuelve a proponer su composición original (CA-43.08). Por eso este caso
 * de uso no conoce `PlanRepository` — ningún camino que salga de aquí alcanza el plan.
 */
class AddExerciseToSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, exerciseId: Long): Long {
        return sessionRepository.addExerciseToSession(sessionId, exerciseId)
    }
}
