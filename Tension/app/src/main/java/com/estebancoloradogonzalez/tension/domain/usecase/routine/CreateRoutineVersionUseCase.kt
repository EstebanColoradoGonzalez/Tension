package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class CreateRoutineVersionUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(routineId: Long) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede crear versiones durante una descarga activa"
        }
        routineRepository.createVersion(routineId)
    }
}
