package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.ModuleWithVersions
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrainingPlanUseCase @Inject constructor(
    private val planRepository: PlanRepository,
) {
    operator fun invoke(): Flow<List<ModuleWithVersions>> =
        planRepository.getModulesWithVersionCounts()
}
