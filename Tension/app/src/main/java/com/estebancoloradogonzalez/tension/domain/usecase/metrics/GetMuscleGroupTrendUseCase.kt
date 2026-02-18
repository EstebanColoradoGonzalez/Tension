package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTrend
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.model.TrendDirection
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionRateRule
import com.estebancoloradogonzalez.tension.domain.rules.TonnageRule
import com.estebancoloradogonzalez.tension.domain.rules.TrendClassificationRule
import javax.inject.Inject

class GetMuscleGroupTrendUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(
        microcycleMap: Map<Int, List<Long>>,
    ): List<MuscleGroupTrend> {
        val completedMicrocycles = microcycleMap.filter { it.value.size == 6 }
        if (completedMicrocycles.size < 4) return emptyList()

        val recentMicrocycles = completedMicrocycles.entries
            .sortedByDescending { it.key }
            .take(6)
            .sortedBy { it.key }

        val tonnageSnapshots = recentMicrocycles.map { (number, sessionIds) ->
            val data = metricsRepository.getTonnageDataBySessionIds(sessionIds)
            val sets = data.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) }
            number to TonnageRule.calculateForMuscleGroup(sets)
        }

        val progressionSnapshots = recentMicrocycles.mapNotNull { (number, sessionIds) ->
            val counts = metricsRepository.getClassificationCountsBySessionIds(sessionIds)
            if (counts.isEmpty()) return@mapNotNull null
            val ratesByGroup = counts.groupBy { it.muscleGroup }
                .mapValues { (_, groupCounts) ->
                    val totalPositive = groupCounts.sumOf { it.positiveCount }
                    val totalAll = groupCounts.sumOf { it.totalCount }
                    ProgressionRateRule.calculate(totalPositive, totalAll)
                }
            number to ratesByGroup
        }

        val allGroups = ALL_GROUPS

        return allGroups.map { group ->
            val tonnageValues = tonnageSnapshots.map { it.second[group] ?: 0.0 }
            val tonnageTrend = TrendClassificationRule.classify(tonnageValues)

            val rateValues = progressionSnapshots.map { it.second[group] ?: 0.0 }
            val rateTrend = if (rateValues.size >= 2) {
                TrendClassificationRule.classify(rateValues)
            } else {
                TrendDirection.STABLE
            }

            val combined = when {
                tonnageTrend == TrendDirection.DECLINING ||
                    rateTrend == TrendDirection.DECLINING -> TrendDirection.DECLINING
                tonnageTrend == TrendDirection.STABLE ||
                    rateTrend == TrendDirection.STABLE -> TrendDirection.STABLE
                else -> TrendDirection.ASCENDING
            }
            MuscleGroupTrend(group, combined)
        }
    }

    companion object {
        private val ALL_GROUPS = listOf(
            "Pecho", "Espalda", "Abdomen", "Hombro", "Tríceps", "Bíceps",
            "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos",
        )
    }
}
