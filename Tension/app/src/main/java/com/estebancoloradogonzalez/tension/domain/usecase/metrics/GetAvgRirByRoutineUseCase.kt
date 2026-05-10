package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.rules.AlertThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
import javax.inject.Inject

class GetAvgRirByRoutineUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
    private val planRepository: PlanRepository,
) {

    suspend operator fun invoke(sessionLimit: Int = 2): List<RirByRoutine> {
        val routines = planRepository.getAllRoutines()
        return routines.map { routine ->
            val rirValues = metricsRepository.getRirValuesByRoutine(routine.id, sessionLimit)
            if (rirValues.isEmpty()) {
                return@map RirByRoutine(routine.id, routine.name, null, null)
            }
            val avg = AvgRirRule.calculate(rirValues)
            val interpretation = when {
                avg < AlertThresholdRule.RIR_LOW_THRESHOLD -> RirInterpretation.RISK_TOO_CLOSE
                avg > AlertThresholdRule.RIR_HIGH_THRESHOLD -> RirInterpretation.INSUFFICIENT_STIMULUS
                else -> RirInterpretation.OPTIMAL
            }
            RirByRoutine(routine.id, routine.name, avg, interpretation)
        }
    }
}
