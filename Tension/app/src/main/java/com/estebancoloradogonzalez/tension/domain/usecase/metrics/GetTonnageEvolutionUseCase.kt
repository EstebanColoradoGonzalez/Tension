package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.TonnageRule
import javax.inject.Inject

class GetTonnageEvolutionUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(
        microcycleMap: Map<Int, List<Long>>,
    ): List<TonnageSnapshot> {
        return microcycleMap.map { (number, sessionIds) ->
            val tonnageData = metricsRepository.getTonnageDataBySessionIds(sessionIds)
            val sets = tonnageData.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) }
            val tonnageMap = TonnageRule.calculateForMuscleGroup(sets)
            TonnageSnapshot(number, tonnageMap)
        }
    }
}
