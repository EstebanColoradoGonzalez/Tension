package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import javax.inject.Inject

class UpdatePlanAssignmentUseCase @Inject constructor(
    private val planRepository: PlanRepository,
) {
    suspend operator fun invoke(
        routineVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
    ) {
        require(sets in 1..10) { "Sets must be between 1 and 10" }
        require(isValidReps(reps)) { "Invalid reps format" }
        planRepository.updatePlanAssignment(routineVersionId, exerciseId, sets, reps)
    }

    private fun isValidReps(reps: String): Boolean {
        if (reps == "TO_TECHNICAL_FAILURE" || reps == "30-45_SEC") return true
        val parts = reps.split("-")
        return parts.size == 2 &&
            parts[0].toIntOrNull() != null &&
            parts[1].toIntOrNull() != null &&
            parts[0].toInt() > 0 &&
            parts[0].toInt() < parts[1].toInt()
    }
}
