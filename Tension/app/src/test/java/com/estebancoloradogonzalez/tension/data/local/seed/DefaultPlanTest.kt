package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPlanTest {

    private fun routine(routineVersionId: Long): List<Triple<Long, Int, Int>> =
        DefaultPlan.ASSIGNMENTS
            .filter { it.routineVersionId == routineVersionId }
            .sortedBy { it.sortOrder }
            .map { Triple(it.exerciseId, it.sets, it.slot) }

    private fun slotsOf(routineVersionId: Long): Map<Int, List<SeedAssignment>> =
        DefaultPlan.ASSIGNMENTS.filter { it.routineVersionId == routineVersionId }.groupBy { it.slot }

    // CA-29.01 — Composición del plan por defecto

    @Test
    fun `plan has six routines with sequential sort order`() {
        assertEquals(6, DefaultPlan.ROUTINES.size)
        assertEquals((1..6).toList(), DefaultPlan.ROUTINES.map { it.sortOrder })
        assertEquals((1L..6L).toList(), DefaultPlan.ROUTINES.map { it.id })
    }

    @Test
    fun `routine names reflect their focus`() {
        val names = DefaultPlan.ROUTINES.associate { it.id to it.name }
        assertEquals("Push — Foco Deltoides Lateral y Medio", names[1L])
        assertEquals("Pull — Foco Dorsal Ancho", names[2L])
        assertEquals("Lower — Foco Cuádriceps", names[3L])
        assertEquals("Push — Foco Tríceps", names[4L])
        assertEquals("Pull — Foco Trapecios y Espalda Media", names[5L])
        assertEquals("Lower — Foco Isquiotibiales y Glúteo", names[6L])
    }

    // CA-36.01 — El nombre de la rutina deja de depender del día

    @Test
    fun `no routine name mentions a week day`() {
        val weekDayWords = listOf(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo",
        )

        DefaultPlan.ROUTINES.forEach { routine ->
            weekDayWords.forEach { day ->
                assertFalse(
                    "El nombre \"${routine.name}\" vuelve a nombrar el día $day",
                    routine.name.contains(day, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `plan has 35 assignments`() {
        assertEquals(35, DefaultPlan.ASSIGNMENTS.size)
    }

    @Test
    fun `every assignment uses the 8-12 rep range`() {
        assertTrue(DefaultPlan.ASSIGNMENTS.all { it.reps == "8-12" })
    }

    @Test
    fun `monday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(10L, 4, 1), // Elevación Lateral
                Triple(18L, 3, 2), // Press de Banca Inclinado
                Triple(34L, 3, 2), // Press Militar
                Triple(19L, 3, 3), // Press de Banca Plano
                Triple(28L, 3, 4), // Aperturas
            ),
            routine(1),
        )
    }

    @Test
    fun `tuesday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(25L, 4, 1), // Jalón al Pecho
                Triple(35L, 4, 1), // Dominadas
                Triple(30L, 3, 2), // Curl Martillo
                Triple(36L, 3, 3), // Remo Unilateral en Polea Baja
                Triple(4L, 3, 4), // Curl Bayesian en Banco Inclinado
                Triple(29L, 3, 5), // Pull-Over
                Triple(3L, 3, 6), // Crunch Abdominal
            ),
            routine(2),
        )
    }

    @Test
    fun `wednesday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(11L, 4, 1), // Extensión de Cuádriceps
                Triple(24L, 3, 2), // Sentadilla Hack
                Triple(17L, 3, 2), // Prensa Inclinada
                Triple(22L, 3, 3), // Sentadilla Búlgara
                Triple(1L, 3, 4), // Aductores
                Triple(9L, 3, 5), // Elevación de Pantorrilla
            ),
            routine(3),
        )
    }

    @Test
    fun `thursday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(13L, 4, 1), // Extensión de Tríceps por encima de la Cabeza
                Triple(19L, 3, 2), // Press de Banca Plano
                Triple(28L, 3, 3), // Aperturas
                Triple(12L, 3, 4), // Extensión de Tríceps en Polea (Pushdown)
                Triple(31L, 3, 5), // Rompecráneos
            ),
            routine(4),
        )
    }

    @Test
    fun `friday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(21L, 4, 1), // Remo T Inclinado
                Triple(14L, 3, 2), // Face Pull
                Triple(26L, 3, 2), // Vuelos Posteriores
                Triple(32L, 3, 3), // Remo Horizontal
                Triple(37L, 3, 4), // Remo Unilateral en Polea Alta
                Triple(8L, 3, 5), // Curl de Predicador
                Triple(3L, 3, 6), // Crunch Abdominal
            ),
            routine(5),
        )
    }

    @Test
    fun `saturday composition matches the story`() {
        assertEquals(
            listOf(
                Triple(6L, 4, 1), // Curl de Isquiotibiales Sentado
                Triple(16L, 3, 2), // Peso Muerto Rumano
                Triple(15L, 3, 3), // Hip Thrust
                Triple(1L, 3, 4), // Aductores
                Triple(9L, 3, 5), // Elevación de Pantorrilla
            ),
            routine(6),
        )
    }

    @Test
    fun `exercises removed from the plan are no longer assigned`() {
        val assigned = DefaultPlan.ASSIGNMENTS.map { it.exerciseId }.toSet()
        assertTrue("Remo al Mentón sigue en el plan", 27L !in assigned)
        assertTrue("Zancadas sigue en el plan", 33L !in assigned)
        assertTrue("Extensión de Cuádriceps sigue asignada al sábado", routine(6).none { it.first == 11L })
    }

    // CA-29.04 — Slots duales

    // El slot es la unidad de conteo de ejercicios de una versión

    @Test
    fun `each routine prescribes fewer exercises than assignments where a slot is dual`() {
        // Fija la expectativa que la consulta de conteo debe cumplir: un slot dual son dos
        // asignaciones y **un** ejercicio de la sesión — o se hace uno o se hace el otro.
        val expectedSlotsByRoutine = mapOf(1L to 4, 2L to 6, 3L to 5, 4L to 5, 5L to 6, 6L to 5)

        expectedSlotsByRoutine.forEach { (routineVersionId, expectedSlots) ->
            assertEquals(
                "La rutina $routineVersionId debe prescribir $expectedSlots ejercicios",
                expectedSlots,
                slotsOf(routineVersionId).size,
            )
        }
    }

    @Test
    fun `counting assignments instead of slots would inflate the four dual-slot routines`() {
        val routinesWithDualSlot = listOf(1L, 2L, 3L, 5L)

        routinesWithDualSlot.forEach { routineVersionId ->
            val assignments = DefaultPlan.ASSIGNMENTS.count { it.routineVersionId == routineVersionId }
            assertEquals(
                "La rutina $routineVersionId tiene un slot dual: una asignación de más",
                assignments - 1,
                slotsOf(routineVersionId).size,
            )
        }
    }

    @Test
    fun `plan defines exactly four dual slots`() {
        val dualSlots = DefaultPlan.ASSIGNMENTS
            .groupBy { it.routineVersionId to it.slot }
            .filterValues { it.size > 1 }
        assertEquals(4, dualSlots.size)
        assertEquals(
            listOf(1L to 2, 2L to 1, 3L to 2, 5L to 2),
            dualSlots.keys.sortedWith(compareBy({ it.first }, { it.second })),
        )
    }

    @Test
    fun `no slot holds more than two exercises`() {
        DefaultPlan.ASSIGNMENTS
            .groupBy { it.routineVersionId to it.slot }
            .forEach { (key, assignments) -> assertTrue("Slot $key con ${assignments.size}", assignments.size <= 2) }
    }

    @Test
    fun `dual slot pairs share sets and reps`() {
        DefaultPlan.ASSIGNMENTS
            .groupBy { it.routineVersionId to it.slot }
            .filterValues { it.size > 1 }
            .forEach { (key, pair) ->
                assertEquals("Series distintas en $key", 1, pair.map { it.sets }.distinct().size)
                assertEquals("Repeticiones distintas en $key", 1, pair.map { it.reps }.distinct().size)
            }
    }

    @Test
    fun `primary of each dual slot is the first one listed`() {
        val primaries = DefaultPlan.ASSIGNMENTS
            .groupBy { it.routineVersionId to it.slot }
            .filterValues { it.size > 1 }
            .mapValues { (_, pair) -> pair.minByOrNull { it.sortOrder }!!.exerciseId }
        assertEquals(18L, primaries[1L to 2]) // Press de Banca Inclinado sobre Press Militar
        assertEquals(25L, primaries[2L to 1]) // Jalón al Pecho sobre Dominadas
        assertEquals(24L, primaries[3L to 2]) // Sentadilla Hack sobre Prensa Inclinada
        assertEquals(14L, primaries[5L to 2]) // Face Pull sobre Vuelos Posteriores
    }

    @Test
    fun `curl bayesian occupies a single exercise slot`() {
        val slot = slotsOf(2).values.first { assignments -> assignments.any { it.exerciseId == 4L } }
        assertEquals(1, slot.size)
    }

    // Integridad estructural

    @Test
    fun `every assigned exercise exists in the catalog`() {
        DefaultPlan.ASSIGNMENTS.forEach { assignment ->
            assertNotNull(
                "El ejercicio ${assignment.exerciseId} no existe en el catálogo",
                ExerciseCatalog.byId(assignment.exerciseId),
            )
        }
    }

    @Test
    fun `no duplicated routine version and exercise pair`() {
        val keys = DefaultPlan.ASSIGNMENTS.map { it.routineVersionId to it.exerciseId }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun `sort order is sequential within each routine version`() {
        DefaultPlan.ROUTINES.forEach { routineSeed ->
            val sortOrders = DefaultPlan.ASSIGNMENTS
                .filter { it.routineVersionId == routineSeed.id }
                .map { it.sortOrder }
                .sorted()
            assertEquals((1..sortOrders.size).toList(), sortOrders)
        }
    }

    @Test
    fun `slots are sequential within each routine version`() {
        DefaultPlan.ROUTINES.forEach { routineSeed ->
            val slots = DefaultPlan.ASSIGNMENTS
                .filter { it.routineVersionId == routineSeed.id }
                .map { it.slot }
                .distinct()
                .sorted()
            assertEquals((1..slots.size).toList(), slots)
        }
    }
}
