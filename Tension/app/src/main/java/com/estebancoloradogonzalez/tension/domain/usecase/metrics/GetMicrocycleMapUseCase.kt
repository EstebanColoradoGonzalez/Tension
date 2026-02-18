package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import javax.inject.Inject

class GetMicrocycleMapUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(): Map<Int, List<Long>> =
        metricsRepository.getSessionIdsGroupedByMicrocycle()
}
