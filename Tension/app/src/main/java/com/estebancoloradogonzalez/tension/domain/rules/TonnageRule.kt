package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage

object TonnageRule {

    fun calculateForMuscleGroup(
        sets: List<SetForTonnage>,
    ): Map<String, Double> {
        return sets.groupBy { it.muscleGroup }
            .mapValues { (_, groupSets) ->
                groupSets.sumOf { it.weightKg * it.reps }
            }
    }
}
