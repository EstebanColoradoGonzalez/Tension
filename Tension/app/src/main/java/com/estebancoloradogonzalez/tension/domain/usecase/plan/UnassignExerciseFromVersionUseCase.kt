package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import javax.inject.Inject

class UnassignExerciseFromVersionUseCase @Inject constructor(
    private val planRepository: PlanRepository,
) {
    suspend operator fun invoke(moduleVersionId: Long, exerciseId: Long) {
        planRepository.unassignExercise(moduleVersionId, exerciseId)
    }
}
