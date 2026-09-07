package com.estebancoloradogonzalez.tension.domain.model

data class SetData(
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val captureUnit: WeightUnit = WeightUnit.KG,
    /**
     * Implemento con el que se ejecutó la serie (HU-39).
     *
     * Vacío por defecto porque el modelo tiene dos lectores y solo uno lo necesita: el
     * detalle de sesión pasada lo muestra, y el motor de progresión —que consume estas
     * series para clasificar— no lo lee. Poner el implemento donde no se usa obligaría a
     * resolverlo en consultas que hoy no lo consultan, sin que nadie mirara el valor.
     */
    val equipmentTypeName: String = "",
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
