package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class AssignExerciseToVersionUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede modificar el plan durante una descarga activa"
        }
        require(!sessionRepository.hasActiveSessionForVersion(routineVersionId)) {
            "No se puede modificar el plan mientras hay una sesión activa de esta versión"
        }
        require(sets > 0) { "Sets must be greater than 0" }
        require(reps in VALID_REPS) { "Invalid reps value: $reps" }
        planRepository.assignExercise(routineVersionId, exerciseId, sets, reps)
    }

    companion object {
        private val VALID_REPS = setOf("8-12", "TO_TECHNICAL_FAILURE", "30-45_SEC")
    }
}
