package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class DeleteRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(id: Long) {
        require(routineRepository.countRoutines() > 1) {
            "No se puede eliminar la última rutina"
        }
        require(!routineRepository.hasActiveSessionForRoutine(id)) {
            "No se puede eliminar una rutina con sesión activa"
        }
        require(!routineRepository.hasSessionsForRoutine(id)) {
            "No se puede eliminar una rutina con historial de sesiones"
        }
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede eliminar una rutina durante una descarga activa"
        }
        routineRepository.deleteRoutine(id)
    }
}
