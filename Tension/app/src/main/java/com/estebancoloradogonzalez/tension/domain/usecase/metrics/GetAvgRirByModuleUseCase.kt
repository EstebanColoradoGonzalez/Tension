package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.RirByModule
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
import javax.inject.Inject

class GetAvgRirByModuleUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(sessionLimit: Int = 2): List<RirByModule> {
        return listOf("A", "B", "C").map { moduleCode ->
            val rirValues = metricsRepository.getRirValuesByModule(moduleCode, sessionLimit)
            if (rirValues.isEmpty()) {
                return@map RirByModule(moduleCode, null, null)
            }
            val avg = AvgRirRule.calculate(rirValues)
            val interpretation = when {
                avg < 1.5 -> RirInterpretation.RISK_TOO_CLOSE
                avg > 3.5 -> RirInterpretation.INSUFFICIENT_STIMULUS
                else -> RirInterpretation.OPTIMAL
            }
            RirByModule(moduleCode, avg, interpretation)
        }
    }
}
