package com.estebancoloradogonzalez.tension.domain.model

data class SetData(
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
)

data class ExerciseSessionData(
    val sets: List<SetData>,
) {
    val setCount: Int get() = sets.size

    val avgWeightKg: Double
        get() = if (sets.isEmpty()) 0.0 else sets.sumOf { it.weightKg } / sets.size

    val totalReps: Int
        get() = sets.sumOf { it.reps }

    val avgRir: Double
        get() = if (sets.isEmpty()) 0.0 else sets.sumOf { it.rir.toDouble() } / sets.size
}
