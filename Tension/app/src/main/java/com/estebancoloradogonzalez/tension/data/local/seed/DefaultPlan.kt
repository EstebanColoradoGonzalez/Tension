package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedAssignment
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedRoutine

/**
 * Plan de entrenamiento predeterminado: 6 rutinas, una versión por rutina, 35 asignaciones.
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
 * comparten series y repeticiones, pero **no** el equipamiento sugerido: son ejercicios
 * distintos con opciones distintas.
 *
 * Desde HU-41 cada asignación lleva su **equipamiento sugerido**, que preselecciona el
 * selector al registrar la serie sin imponerlo. La sugerencia es el valor atómico de la
 * primera opción que el ejercicio lista en [ExerciseCatalog], y siempre una de las que
 * admite. HU-41 trajo además tres cambios de composición: `Aductores` pasa al primer
 * puesto del miércoles y del sábado, y el cuarto puesto del viernes deja de ser
 * `Remo Unilateral Polea Alta` para ser `Trapecios con Apoyo en Banco Inclinado`.
 */
object DefaultPlan {

    const val REPS_8_12 = "8-12"

    private const val MAQUINA = EquipmentCatalog.MAQUINA
    private const val POLEA = EquipmentCatalog.POLEA
    private const val BARRA = EquipmentCatalog.BARRA
    private const val BARRA_FIJA = EquipmentCatalog.BARRA_FIJA
    private const val MANCUERNA = EquipmentCatalog.MANCUERNA

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
        pa(rv = 1, exerciseId = 10, sets = 4, sortOrder = 1, slot = 1, eq = MANCUERNA), // Elevación Lateral
        pa(rv = 1, exerciseId = 18, sets = 3, sortOrder = 2, slot = 2, eq = BARRA), // Press de Banca Inclinado (primario)
        pa(rv = 1, exerciseId = 34, sets = 3, sortOrder = 3, slot = 2, eq = BARRA), // Press Militar (alternativa)
        pa(rv = 1, exerciseId = 19, sets = 3, sortOrder = 4, slot = 3, eq = BARRA), // Press de Banca Plano
        pa(rv = 1, exerciseId = 28, sets = 3, sortOrder = 5, slot = 4, eq = MAQUINA), // Aperturas

        // ===== Rutina 2 — Pull Foco Dorsal Ancho =====
        // El slot dual no comparte sugerencia: la dominada no se hace en polea.
        pa(rv = 2, exerciseId = 25, sets = 4, sortOrder = 1, slot = 1, eq = POLEA), // Jalón al Pecho (primario)
        pa(rv = 2, exerciseId = 35, sets = 4, sortOrder = 2, slot = 1, eq = BARRA_FIJA), // Dominadas (alternativa)
        pa(rv = 2, exerciseId = 30, sets = 3, sortOrder = 3, slot = 2, eq = MANCUERNA), // Curl Martillo
        pa(rv = 2, exerciseId = 36, sets = 3, sortOrder = 4, slot = 3, eq = POLEA), // Remo Unilateral Polea Baja
        pa(rv = 2, exerciseId = 4, sets = 3, sortOrder = 5, slot = 4, eq = MANCUERNA), // Curl Bayesian en Banco Inclinado
        pa(rv = 2, exerciseId = 29, sets = 3, sortOrder = 6, slot = 5, eq = POLEA), // Pull-Over
        pa(rv = 2, exerciseId = 3, sets = 3, sortOrder = 7, slot = 6, eq = POLEA), // Crunch Abdominal

        // ===== Rutina 3 — Lower Foco Cuádriceps =====
        // HU-41: Aductores pasa del cuarto al primer puesto; el resto conserva su orden.
        pa(rv = 3, exerciseId = 1, sets = 3, sortOrder = 1, slot = 1, eq = MAQUINA), // Aductores
        pa(rv = 3, exerciseId = 11, sets = 4, sortOrder = 2, slot = 2, eq = MAQUINA), // Extensión de Cuádriceps
        pa(rv = 3, exerciseId = 24, sets = 3, sortOrder = 3, slot = 3, eq = MAQUINA), // Sentadilla Hack (primario)
        pa(rv = 3, exerciseId = 17, sets = 3, sortOrder = 4, slot = 3, eq = MAQUINA), // Prensa Inclinada (alternativa)
        pa(rv = 3, exerciseId = 22, sets = 3, sortOrder = 5, slot = 4, eq = MANCUERNA), // Sentadilla Búlgara
        pa(rv = 3, exerciseId = 9, sets = 3, sortOrder = 6, slot = 5, eq = MAQUINA), // Elevación de Pantorrilla

        // ===== Rutina 4 — Push Foco Tríceps =====
        pa(rv = 4, exerciseId = 13, sets = 4, sortOrder = 1, slot = 1, eq = MANCUERNA), // Extensión de Tríceps sobre Cabeza
        pa(rv = 4, exerciseId = 19, sets = 3, sortOrder = 2, slot = 2, eq = BARRA), // Press de Banca Plano
        pa(rv = 4, exerciseId = 28, sets = 3, sortOrder = 3, slot = 3, eq = MAQUINA), // Aperturas
        pa(rv = 4, exerciseId = 12, sets = 3, sortOrder = 4, slot = 4, eq = POLEA), // Extensión de Tríceps (Pushdown)
        pa(rv = 4, exerciseId = 31, sets = 3, sortOrder = 5, slot = 5, eq = MANCUERNA), // Rompecráneos

        // ===== Rutina 5 — Pull Foco Trapecios y Espalda Media =====
        // HU-41: el cuarto puesto deja de ser Remo Unilateral Polea Alta (37) y pasa a ser
        // Trapecios con Apoyo en Banco Inclinado (38). El 37 permanece en el Diccionario.
        pa(rv = 5, exerciseId = 21, sets = 4, sortOrder = 1, slot = 1, eq = MAQUINA), // Remo T Inclinado
        pa(rv = 5, exerciseId = 14, sets = 3, sortOrder = 2, slot = 2, eq = POLEA), // Face Pull (primario)
        pa(rv = 5, exerciseId = 26, sets = 3, sortOrder = 3, slot = 2, eq = MANCUERNA), // Vuelos Posteriores (alternativa)
        pa(rv = 5, exerciseId = 32, sets = 3, sortOrder = 4, slot = 3, eq = POLEA), // Remo Horizontal
        pa(rv = 5, exerciseId = 38, sets = 3, sortOrder = 5, slot = 4, eq = MANCUERNA), // Trapecios con Apoyo en Banco Inclinado
        pa(rv = 5, exerciseId = 8, sets = 3, sortOrder = 6, slot = 5, eq = MANCUERNA), // Curl de Predicador
        pa(rv = 5, exerciseId = 3, sets = 3, sortOrder = 7, slot = 6, eq = POLEA), // Crunch Abdominal

        // ===== Rutina 6 — Lower Foco Isquiotibiales y Glúteo =====
        // HU-41: Aductores pasa del cuarto al primer puesto; el resto conserva su orden.
        pa(rv = 6, exerciseId = 1, sets = 3, sortOrder = 1, slot = 1, eq = MAQUINA), // Aductores
        pa(rv = 6, exerciseId = 6, sets = 4, sortOrder = 2, slot = 2, eq = MAQUINA), // Curl de Isquiotibiales Sentado
        pa(rv = 6, exerciseId = 16, sets = 3, sortOrder = 3, slot = 3, eq = MANCUERNA), // Peso Muerto Rumano
        pa(rv = 6, exerciseId = 15, sets = 3, sortOrder = 4, slot = 4, eq = MANCUERNA), // Hip Thrust
        pa(rv = 6, exerciseId = 9, sets = 3, sortOrder = 5, slot = 5, eq = MAQUINA), // Elevación de Pantorrilla
    )

    private fun pa(
        rv: Long,
        exerciseId: Long,
        sets: Int,
        sortOrder: Int,
        slot: Int,
        eq: Long,
    ) = SeedAssignment(
        routineVersionId = rv,
        exerciseId = exerciseId,
        sets = sets,
        reps = REPS_8_12,
        sortOrder = sortOrder,
        slot = slot,
        suggestedEquipmentTypeId = eq,
    )
}
