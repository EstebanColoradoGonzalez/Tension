package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ExercisePairSummary
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSummaryItem
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionSummary
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.ActionSignalRule
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import com.estebancoloradogonzalez.tension.domain.rules.HypertrophyRangeRule
import com.estebancoloradogonzalez.tension.domain.rules.RangeReading
import com.estebancoloradogonzalez.tension.domain.rules.RangeSignal
import com.estebancoloradogonzalez.tension.domain.rules.TopRangeStreak
import javax.inject.Inject

class GetSessionSummaryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): SessionSummary {
        val data = sessionRepository.getSessionSummaryData(sessionId)
        val pairsBySessionExercise = data.pairs.groupBy { it.sessionExerciseId }

        val items = data.exercises.map { dto ->
            val isDeload = dto.isDeload == 1
            val classification = dto.classification?.let {
                ProgressionClassification.valueOf(it)
            }

            // Un renglón por implemento efectivamente usado (CA-40.02). La señal se resuelve
            // **por par**, con la carga que el motor prescribió para ese par: mostrar la del
            // ejercicio devolvería a la pantalla el peso de otro implemento, que es
            // exactamente el defecto que esta historia corrige.
            val pairDtos = pairsBySessionExercise[dto.sessionExerciseId].orEmpty()
            val pairs = pairDtos.map { pair ->
                val pairClassification = pair.classification?.let {
                    ProgressionClassification.valueOf(it)
                }
                val rangeSignal = rangeSignalOf(
                    rangeReading = pair.rangeReading,
                    topRangeStreak = pair.topRangeStreak,
                    equipmentName = pair.equipmentTypeName,
                    admitsAddedWeight = pair.admitsAddedWeight,
                    isBodyweight = dto.isBodyweight == 1,
                    isIsometric = dto.isIsometric == 1,
                )
                ExercisePairSummary(
                    equipmentTypeId = pair.equipmentTypeId,
                    equipmentTypeName = pair.equipmentTypeName,
                    classification = pairClassification,
                    signal = ActionSignalRule.resolve(
                        classification = pairClassification,
                        prescribedLoadKg = pair.prescribedLoadKg,
                        avgWeightKg = pair.avgWeightKg,
                        routineRequiresDeload = data.routineRequiresDeload,
                        isBodyweight = dto.isBodyweight == 1,
                        isIsometric = dto.isIsometric == 1,
                        totalReps = pair.totalReps,
                        previousTotalReps = pair.previousTotalReps,
                        setCount = pair.setCount,
                        isMastered = dto.isMastered == 1,
                        isDeload = isDeload,
                        rangeSignal = rangeSignal,
                    ),
                    weightKg = pair.avgWeightKg,
                    completedSets = pair.setCount,
                )
            }

            // Con un solo implemento la señal del ejercicio es la de su par, y la de rango
            // manda (HU-46). Con varios, cada renglón lleva la suya y la del ejercicio se sigue
            // resolviendo sobre su lectura consolidada (CA-40.08). La carga prescrita que la
            // acompaña es la del par de mayor objetivo, coherente con la disyunción de CA-40.04.
            val signal = pairs.singleOrNull()?.signal ?: ActionSignalRule.resolve(
                classification = classification,
                prescribedLoadKg = pairDtos.mapNotNull { it.prescribedLoadKg }.maxOrNull(),
                avgWeightKg = dto.avgWeightKg,
                routineRequiresDeload = data.routineRequiresDeload,
                isBodyweight = dto.isBodyweight == 1,
                isIsometric = dto.isIsometric == 1,
                totalReps = dto.totalReps,
                previousTotalReps = dto.previousTotalReps,
                setCount = dto.setCount,
                isMastered = dto.isMastered == 1,
                isDeload = isDeload,
            )

            ExerciseSummaryItem(
                exerciseId = dto.exerciseId,
                name = dto.exerciseName,
                classification = classification,
                signal = signal,
                weightKg = dto.avgWeightKg,
                isBodyweight = dto.isBodyweight == 1,
                isIsometric = dto.isIsometric == 1,
                isMastered = dto.isMastered == 1,
                isDeload = isDeload,
                completedSets = dto.setCount,
                prescribedSets = dto.prescribedSets,
                pairs = pairs,
            )
        }

        return SessionSummary(
            status = data.info.status,
            routineName = data.info.routineName,
            versionNumber = data.info.versionNumber,
            totalTonnageKg = data.info.totalTonnageKg,
            completedExercises = data.info.completedExercises,
            totalExercises = data.info.totalExercises,
            exercises = items,
        )
    }

    /**
     * Lectura del par contra el rango 8–12, ya convertida en señal (HU-46).
     *
     * `null` cuando el cierre no la evaluó —isométricos y descarga— y cuando el par no tiene
     * ninguna señal de peso: sin carga externa y sin admitir «Peso Añadido» (CA-46.15). En
     * esos casos el resumen conserva la lectura de siempre.
     */
    @Suppress("LongParameterList")
    private fun rangeSignalOf(
        rangeReading: String?,
        topRangeStreak: Int?,
        equipmentName: String,
        admitsAddedWeight: Boolean,
        isBodyweight: Boolean,
        isIsometric: Boolean,
    ): RangeSignal? {
        val reading = RangeReading.fromCode(rangeReading) ?: return null
        val hasExternalLoad = ExternalLoadRule.isCaptureEnabled(
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            equipmentName = equipmentName,
        )
        if (!hasExternalLoad && !admitsAddedWeight) return null
        return HypertrophyRangeRule.sessionSignal(
            reading = reading,
            streak = TopRangeStreak(count = topRangeStreak ?: 0, referenceWeightKg = null),
            hasExternalLoad = hasExternalLoad,
            admitsAddedWeight = admitsAddedWeight,
        )
    }
}
