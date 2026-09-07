package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.rules.LoadVelocityRule
import java.time.LocalDate
import javax.inject.Inject

/**
 * Velocidad de carga por ejercicio, calculada **por par y consolidada por máximo** (HU-40).
 *
 * El KPI compara pesos entre sesiones, así que la pendiente se traza sobre cada implemento
 * por separado: entre 20 kg de polea y 12 kg de mancuerna no hay pendiente, hay dos
 * magnitudes distintas.
 *
 * CA-40.07 manda consolidar «según CA-40.04», pero esa consolidación está enunciada en
 * términos booleanos y esto es un número en kg/sesión. La traducción fiel del principio
 * —*el ejercicio progresa si alguno de sus pares progresó*— es conservar la **mayor** de
 * las velocidades: promediarlas dejaría que un implemento parado borrase a uno que avanza,
 * que es exactamente la dilución que la historia existe para evitar. `sessionCount` es el
 * del par que gana, porque es la evidencia de suficiencia del número que se muestra.
 */
class GetLoadVelocityUseCase @Inject constructor(
    private val metricsRepository: MetricsRepository,
) {

    suspend operator fun invoke(weeksBack: Int = 4): List<ExerciseLoadVelocity> {
        val startDate = LocalDate.now().minusWeeks(weeksBack.toLong()).toString()
        val ranges = metricsRepository.getPairSessionRanges(startDate)

        return ranges.groupBy { it.exerciseId }.mapNotNull { (_, pairRanges) ->
            val reference = pairRanges.first()
            if (reference.isBodyweight == 1 || reference.isIsometric == 1) {
                return@mapNotNull ExerciseLoadVelocity(
                    exerciseId = reference.exerciseId,
                    exerciseName = reference.exerciseName,
                    velocity = 0.0,
                    isBodyweight = true,
                    sessionCount = pairRanges.maxOf { it.sessionCount },
                )
            }

            val best = pairRanges.map { range ->
                val initialWeight = metricsRepository.getAvgWeightForPairInSession(
                    range.exerciseId,
                    range.equipmentTypeId,
                    range.firstSessionId,
                ) ?: 0.0
                val currentWeight = metricsRepository.getAvgWeightForPairInSession(
                    range.exerciseId,
                    range.equipmentTypeId,
                    range.lastSessionId,
                ) ?: 0.0
                val velocity = LoadVelocityRule.calculate(
                    currentWeight,
                    initialWeight,
                    range.sessionCount,
                )
                velocity to range.sessionCount
            }.maxByOrNull { it.first } ?: return@mapNotNull null

            ExerciseLoadVelocity(
                exerciseId = reference.exerciseId,
                exerciseName = reference.exerciseName,
                velocity = best.first,
                isBodyweight = false,
                sessionCount = best.second,
            )
        }
    }
}
