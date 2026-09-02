package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedAssignment
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedRoutine

/**
 * Plan de entrenamiento predeterminado: 6 rutinas, una versión por rutina.
 *
 * Los nombres no nombran el día: desde HU-36 el día de la semana es una entidad
 * (`week_day`) relacionada con la rutina, y [DefaultWeekDays] es quien establece esa
 * relación. El nombre expresa el patrón de movimiento y el enfoque, nada más.
 *
 * Es únicamente el punto de partida de una instalación fresca. El Ejecutante puede
 * crear versiones, asignar o remover ejercicios y agregar alternativas sin restricción.
 *
 * Dos asignaciones que comparten `slot` dentro de la misma versión forman un slot dual
 * (modelo HU-26): el de menor `sortOrder` es el primario y el otro su alternativa. Ambos
 * comparten series y repeticiones.
 */
object DefaultPlan {

    const val REPS_8_12 = "8-12"

    val ROUTINES: List<SeedRoutine> = listOf(
        SeedRoutine(1, "Push — Foco Deltoides Lateral y Medio", 1),
        SeedRoutine(2, "Pull — Foco Dorsal Ancho", 2),
        SeedRoutine(3, "Lower — Foco Cuádriceps", 3),
        SeedRoutine(4, "Push — Foco Tríceps", 4),
        SeedRoutine(5, "Pull — Foco Trapecios y Espalda Media", 5),
        SeedRoutine(6, "Lower — Foco Isquiotibiales y Glúteo", 6),
    )

    val ASSIGNMENTS: List<SeedAssignment> = listOf(
        // ===== Rutina 1 — Push Foco Deltoides Lateral y Medio =====
        pa(rv = 1, exerciseId = 10, sets = 4, sortOrder = 1, slot = 1), // Elevación Lateral
        pa(rv = 1, exerciseId = 18, sets = 3, sortOrder = 2, slot = 2), // Press de Banca Inclinado (primario)
        pa(rv = 1, exerciseId = 34, sets = 3, sortOrder = 3, slot = 2), // Press Militar (alternativa)
        pa(rv = 1, exerciseId = 19, sets = 3, sortOrder = 4, slot = 3), // Press de Banca Plano
        pa(rv = 1, exerciseId = 28, sets = 3, sortOrder = 5, slot = 4), // Aperturas

        // ===== Rutina 2 — Pull Foco Dorsal Ancho =====
        pa(rv = 2, exerciseId = 25, sets = 4, sortOrder = 1, slot = 1), // Jalón al Pecho (primario)
        pa(rv = 2, exerciseId = 35, sets = 4, sortOrder = 2, slot = 1), // Dominadas (alternativa)
        pa(rv = 2, exerciseId = 30, sets = 3, sortOrder = 3, slot = 2), // Curl Martillo
        pa(rv = 2, exerciseId = 36, sets = 3, sortOrder = 4, slot = 3), // Remo Unilateral en Polea Baja
        pa(rv = 2, exerciseId = 4, sets = 3, sortOrder = 5, slot = 4), // Curl Bayesian en Banco Inclinado
        pa(rv = 2, exerciseId = 29, sets = 3, sortOrder = 6, slot = 5), // Pull-Over
        pa(rv = 2, exerciseId = 3, sets = 3, sortOrder = 7, slot = 6), // Crunch Abdominal

        // ===== Rutina 3 — Lower Foco Cuádriceps =====
        pa(rv = 3, exerciseId = 11, sets = 4, sortOrder = 1, slot = 1), // Extensión de Cuádriceps
        pa(rv = 3, exerciseId = 24, sets = 3, sortOrder = 2, slot = 2), // Sentadilla Hack (primario)
        pa(rv = 3, exerciseId = 17, sets = 3, sortOrder = 3, slot = 2), // Prensa Inclinada (alternativa)
        pa(rv = 3, exerciseId = 22, sets = 3, sortOrder = 4, slot = 3), // Sentadilla Búlgara
        pa(rv = 3, exerciseId = 1, sets = 3, sortOrder = 5, slot = 4), // Aductores
        pa(rv = 3, exerciseId = 9, sets = 3, sortOrder = 6, slot = 5), // Elevación de Pantorrilla

        // ===== Rutina 4 — Push Foco Tríceps =====
        pa(rv = 4, exerciseId = 13, sets = 4, sortOrder = 1, slot = 1), // Extensión de Tríceps sobre la Cabeza
        pa(rv = 4, exerciseId = 19, sets = 3, sortOrder = 2, slot = 2), // Press de Banca Plano
        pa(rv = 4, exerciseId = 28, sets = 3, sortOrder = 3, slot = 3), // Aperturas
        pa(rv = 4, exerciseId = 12, sets = 3, sortOrder = 4, slot = 4), // Extensión de Tríceps en Polea (Pushdown)
        pa(rv = 4, exerciseId = 31, sets = 3, sortOrder = 5, slot = 5), // Rompecráneos

        // ===== Rutina 5 — Pull Foco Trapecios y Espalda Media =====
        pa(rv = 5, exerciseId = 21, sets = 4, sortOrder = 1, slot = 1), // Remo T Inclinado
        pa(rv = 5, exerciseId = 14, sets = 3, sortOrder = 2, slot = 2), // Face Pull (primario)
        pa(rv = 5, exerciseId = 26, sets = 3, sortOrder = 3, slot = 2), // Vuelos Posteriores (alternativa)
        pa(rv = 5, exerciseId = 32, sets = 3, sortOrder = 4, slot = 3), // Remo Horizontal
        pa(rv = 5, exerciseId = 37, sets = 3, sortOrder = 5, slot = 4), // Remo Unilateral en Polea Alta
        pa(rv = 5, exerciseId = 8, sets = 3, sortOrder = 6, slot = 5), // Curl de Predicador
        pa(rv = 5, exerciseId = 3, sets = 3, sortOrder = 7, slot = 6), // Crunch Abdominal

        // ===== Rutina 6 — Lower Foco Isquiotibiales y Glúteo =====
        pa(rv = 6, exerciseId = 6, sets = 4, sortOrder = 1, slot = 1), // Curl de Isquiotibiales Sentado
        pa(rv = 6, exerciseId = 16, sets = 3, sortOrder = 2, slot = 2), // Peso Muerto Rumano
        pa(rv = 6, exerciseId = 15, sets = 3, sortOrder = 3, slot = 3), // Hip Thrust
        pa(rv = 6, exerciseId = 1, sets = 3, sortOrder = 4, slot = 4), // Aductores
        pa(rv = 6, exerciseId = 9, sets = 3, sortOrder = 5, slot = 5), // Elevación de Pantorrilla
    )

    private fun pa(rv: Long, exerciseId: Long, sets: Int, sortOrder: Int, slot: Int) = SeedAssignment(
        routineVersionId = rv,
        exerciseId = exerciseId,
        sets = sets,
        reps = REPS_8_12,
        sortOrder = sortOrder,
        slot = slot,
    )
}
