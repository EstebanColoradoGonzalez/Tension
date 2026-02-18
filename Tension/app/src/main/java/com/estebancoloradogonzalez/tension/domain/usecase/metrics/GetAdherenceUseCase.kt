package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.AdherenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class GetAdherenceUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(): AdherenceData {
        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY).toString()
        val weekEnd = today.with(DayOfWeek.SUNDAY).toString()
        val completed = metricsRepository.getSessionsCompletedInWeek(weekStart, weekEnd)
        val planned = metricsRepository.getWeeklyFrequency()
        val percentage = AdherenceRule.calculate(completed, planned)
        return AdherenceData(completed, planned, percentage)
    }
}
