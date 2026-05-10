package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.VolumeDistributionRule
import javax.inject.Inject

class GetVolumeDistributionUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(sessionIds: List<Long>): Map<String, Map<String, Double>> {
        if (sessionIds.isEmpty()) return emptyMap()
        val distribution = metricsRepository.getSetDistributionBySessionIds(sessionIds)
        val globalTotalSets = distribution.sumOf { it.setCount }
        return distribution.groupBy { it.muscleGroup }.mapValues { (_, groupSets) ->
            val setsByZone = groupSets.associate { it.muscleZoneName to it.setCount }
            VolumeDistributionRule.calculate(setsByZone, globalTotalSets)
        }
    }
}
