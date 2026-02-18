package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.LoadVelocityRule
import java.time.LocalDate
import javax.inject.Inject

class GetLoadVelocityUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(weeksBack: Int = 4): List<ExerciseLoadVelocity> {
        val startDate = LocalDate.now().minusWeeks(weeksBack.toLong()).toString()
        val ranges = metricsRepository.getExerciseSessionRanges(startDate)
        return ranges.map { r ->
            if (r.isBodyweight == 1 || r.isIsometric == 1) {
                return@map ExerciseLoadVelocity(
                    exerciseId = r.exerciseId,
                    exerciseName = r.exerciseName,
                    velocity = 0.0,
                    isBodyweight = true,
                )
            }
            val initialWeight = metricsRepository.getAvgWeightForExerciseInSession(
                r.exerciseId,
                r.firstSessionId,
            ) ?: 0.0
            val currentWeight = metricsRepository.getAvgWeightForExerciseInSession(
                r.exerciseId,
                r.lastSessionId,
            ) ?: 0.0
            val velocity = LoadVelocityRule.calculate(currentWeight, initialWeight, r.sessionCount)
            ExerciseLoadVelocity(
                exerciseId = r.exerciseId,
                exerciseName = r.exerciseName,
                velocity = velocity,
                isBodyweight = false,
            )
        }
    }
}
