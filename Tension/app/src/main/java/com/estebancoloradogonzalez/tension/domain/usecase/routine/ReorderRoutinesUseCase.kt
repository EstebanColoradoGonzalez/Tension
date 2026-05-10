package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReorderRoutinesUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(orderedIds: List<Long>) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede reordenar rutinas durante una descarga activa"
        }
        val hasActive = sessionRepository.getActiveSession().first() != null
        require(!hasActive) {
            "No se puede reordenar rutinas mientras hay una sesión activa"
        }
        routineRepository.reorderRoutines(orderedIds)
    }
}
