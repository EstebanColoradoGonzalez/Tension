package com.estebancoloradogonzalez.tension.domain.model

import com.estebancoloradogonzalez.tension.domain.rules.RangeSignal

data class SessionExerciseDetail(
    val sessionExerciseId: Long,
    val exerciseId: Long?,
    val name: String?,
    val equipmentTypes: List<String>,
    val muscleZones: List<String>,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val prescribedLoadKg: Double?,
    val completedSets: Int,
    val status: ExerciseSessionStatus,
    val muscleGroup: String?,
    val isFinalized: Boolean,
    val pendingSelection: Boolean,
    val slot: Int,
    val hasAlternatives: Boolean = false,
    /** Añadido por el ejecutante a esta sesión, fuera de la estructura de puestos (HU-43). */
    val isExtra: Boolean = false,
    /**
     * Lo que el último cierre dijo del peso de este ejercicio, para mostrarlo junto al peso
     * propuesto (HU-46, CA-46.02, CA-46.04). `null` si nunca se leyó por el rango o si el
     * ejercicio no tiene señal de peso (isométricos, peso corporal sin «Peso Añadido»).
     */
    val rangeSignal: RangeSignal? = null,
    /**
     * Whether this exercise owns the last registered set of the session in progress, the
     * only correctable one (HU-47, CA-47.01). At most one exercise of the session has it
     * (CA-47.07).
     */
    val canCorrectLastSet: Boolean = false,
)
