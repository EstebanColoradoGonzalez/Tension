package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.PlanVersionDetail
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlanVersionDetailUseCase @Inject constructor(
    private val planRepository: PlanRepository,
) {
    operator fun invoke(routineVersionId: Long): Flow<PlanVersionDetail?> =
        planRepository.getVersionDetail(routineVersionId)
}
