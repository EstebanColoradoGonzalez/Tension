package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTonnage
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.TonnageRule
import javax.inject.Inject

class GetTonnageByMuscleGroupUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(sessionIds: List<Long>): List<MuscleGroupTonnage> {
        if (sessionIds.isEmpty()) return ALL_GROUPS.map { MuscleGroupTonnage(it, 0.0) }
        val tonnageData = metricsRepository.getTonnageDataBySessionIds(sessionIds)
        val sets = tonnageData.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) }
        val tonnageMap = TonnageRule.calculateForMuscleGroup(sets)
        return ALL_GROUPS.map { group ->
            MuscleGroupTonnage(group, tonnageMap[group] ?: 0.0)
        }
    }

    companion object {
        val ALL_GROUPS = listOf(
            "Pecho", "Espalda", "Abdomen", "Hombro", "Tríceps", "Bíceps",
            "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos",
        )
    }
}
