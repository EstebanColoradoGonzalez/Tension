package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionRateRule
import java.time.LocalDate
import javax.inject.Inject

class GetProgressionRateUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(weeksBack: Int = 4): List<ExerciseProgressionRate> {
        val startDate = LocalDate.now().minusWeeks(weeksBack.toLong()).toString()
        val counts = metricsRepository.getClassificationCounts(startDate)
        return counts.map { c ->
            ExerciseProgressionRate(
                exerciseId = c.exerciseId,
                exerciseName = c.exerciseName,
                rate = ProgressionRateRule.calculate(c.positiveCount, c.totalCount),
                isBodyweight = c.isBodyweight == 1,
            )
        }
    }
}
