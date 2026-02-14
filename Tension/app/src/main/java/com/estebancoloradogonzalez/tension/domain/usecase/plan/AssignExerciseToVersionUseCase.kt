package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import javax.inject.Inject

class AssignExerciseToVersionUseCase @Inject constructor(
    private val planRepository: PlanRepository,
) {
    suspend operator fun invoke(moduleVersionId: Long, exerciseId: Long, sets: Int, reps: String) {
        require(sets > 0) { "Sets must be greater than 0" }
        require(reps in VALID_REPS) { "Invalid reps value: $reps" }
        planRepository.assignExercise(moduleVersionId, exerciseId, sets, reps)
    }

    companion object {
        private val VALID_REPS = setOf("8-12", "TO_TECHNICAL_FAILURE", "30-45_SEC")
    }
}
