package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class DeleteRoutineVersionUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(versionId: Long, routineId: Long) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede eliminar versiones durante una descarga activa"
        }
        require(routineRepository.countVersionsByRoutine(routineId) > 1) {
            "No se puede eliminar la última versión"
        }
        require(!routineRepository.hasSessionsForVersion(versionId)) {
            "No se puede eliminar una versión con historial de sesiones"
        }
        routineRepository.deleteVersion(versionId, routineId)
    }
}
