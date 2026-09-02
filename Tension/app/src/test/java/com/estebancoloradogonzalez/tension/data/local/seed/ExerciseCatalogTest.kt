package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun `catalog contains exactly 37 exercises`() {
        assertEquals(37, ExerciseCatalog.ALL.size)
    }

    @Test
    fun `exercise ids are unique`() {
        val ids = ExerciseCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `name and equipment pairs are unique`() {
        val pairs = ExerciseCatalog.ALL.map { it.name to it.equipmentTypeId }
        assertEquals(pairs.size, pairs.distinct().size)
    }

    @Test
    fun `exercise muscle zone relations total 41`() {
        val relations = ExerciseCatalog.ALL.sumOf { it.muscleZoneIds.size }
        assertEquals(41, relations)
    }

    @Test
    fun `every exercise has at least one muscle zone within the seeded catalog`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue("${exercise.name} sin zona muscular", exercise.muscleZoneIds.isNotEmpty())
            exercise.muscleZoneIds.forEach { zoneId ->
                assertTrue("${exercise.name} con zona fuera de rango: $zoneId", zoneId in 1L..20L)
            }
            assertEquals(
                "${exercise.name} con zonas duplicadas",
                exercise.muscleZoneIds.size,
                exercise.muscleZoneIds.distinct().size,
            )
        }
    }

    @Test
    fun `only four exercises target two muscle zones`() {
        val multiZone = ExerciseCatalog.ALL.filter { it.muscleZoneIds.size > 1 }.map { it.name }
        assertEquals(
            listOf(
                "Peso Muerto Rumano",
                "Sentadilla Búlgara",
                "Sentadilla de Zumo",
                "Zancadas",
            ),
            multiZone.sorted(),
        )
    }

    @Test
    fun `every exercise declares a media resource`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue("${exercise.name} sin media_resource", exercise.mediaResource.isNotBlank())
        }
    }

    // CA-29.03 — Renombrado de Tirón de Dorsales

    @Test
    fun `jalon al pecho keeps the identity of tiron de dorsales`() {
        val exercise = ExerciseCatalog.byId(25)
        assertNotNull(exercise)
        assertEquals("Jalón al Pecho", exercise!!.name)
        assertEquals(6L, exercise.equipmentTypeId)
        assertEquals("tiron_de_dorsales_polea", exercise.mediaResource)
        assertEquals(listOf(5L), exercise.muscleZoneIds)
    }

    @Test
    fun `tiron de dorsales is not duplicated under its former name`() {
        assertNull(ExerciseCatalog.ALL.firstOrNull { it.name == "Tirón de Dorsales" })
        assertEquals(1, ExerciseCatalog.ALL.count { it.mediaResource == "tiron_de_dorsales_polea" })
    }

    // CA-29.02 / CA-29.06 — Ejercicios nuevos

    @Test
    fun `press militar is registered with dumbbells and shoulder zone`() {
        val exercise = ExerciseCatalog.byId(34)!!
        assertEquals("Press Militar", exercise.name)
        assertEquals(2L, exercise.equipmentTypeId)
        assertEquals(listOf(7L), exercise.muscleZoneIds)
        assertEquals("press_militar_mancuernas", exercise.mediaResource)
        assertFalse(exercise.isBodyweight)
    }

    @Test
    fun `dominadas is registered as a bodyweight lat exercise`() {
        val exercise = ExerciseCatalog.byId(35)!!
        assertEquals("Dominadas", exercise.name)
        assertEquals(23L, exercise.equipmentTypeId)
        assertEquals(listOf(5L), exercise.muscleZoneIds)
        assertEquals("dominadas_barra_fija", exercise.mediaResource)
        assertTrue(exercise.isBodyweight)
    }

    @Test
    fun `remo unilateral en polea baja is registered on middle back`() {
        val exercise = ExerciseCatalog.byId(36)!!
        assertEquals("Remo Unilateral en Polea Baja", exercise.name)
        assertEquals(6L, exercise.equipmentTypeId)
        assertEquals(listOf(4L), exercise.muscleZoneIds)
        assertEquals("remo_unilateral_en_polea_baja_polea", exercise.mediaResource)
    }

    @Test
    fun `remo unilateral en polea alta is registered on upper back`() {
        val exercise = ExerciseCatalog.byId(37)!!
        assertEquals("Remo Unilateral en Polea Alta", exercise.name)
        assertEquals(6L, exercise.equipmentTypeId)
        assertEquals(listOf(16L), exercise.muscleZoneIds)
        assertEquals("remo_unilateral_en_polea_alta_polea", exercise.mediaResource)
    }

    @Test
    fun `dominadas is the only bodyweight exercise of the seed`() {
        val bodyweight = ExerciseCatalog.ALL.filter { it.isBodyweight }.map { it.name }
        assertEquals(listOf("Dominadas"), bodyweight)
    }

    @Test
    fun `no seeded exercise is isometric or to technical failure`() {
        assertTrue(ExerciseCatalog.ALL.none { it.isIsometric })
        assertTrue(ExerciseCatalog.ALL.none { it.isToTechnicalFailure })
    }

    // CA-29.08 — Auditoría de catalogación muscular

    @Test
    fun `remo al menton is catalogued as upper back only`() {
        val exercise = ExerciseCatalog.byId(27)!!
        assertEquals("Remo al Mentón", exercise.name)
        assertEquals(listOf(16L), exercise.muscleZoneIds)
        assertFalse(exercise.muscleZoneIds.contains(7L))
        assertFalse(exercise.muscleZoneIds.contains(17L))
    }

    // CA-29.07 — Preservación del diccionario

    @Test
    fun `exercises dropped from the default plan remain in the dictionary`() {
        listOf(27L, 33L, 11L).forEach { id ->
            assertNotNull("El ejercicio $id fue eliminado del catálogo", ExerciseCatalog.byId(id))
        }
    }

    // CA-29.05 — Variantes de equipamiento

    @Test
    fun `exercise names do not embed their equipment variant`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertFalse(
                "${exercise.name} incorpora la mención del equipamiento",
                exercise.name.contains(" o "),
            )
        }
    }

    // CA-32.05 / CA-32.06 — Dificultad de progresión del catálogo seed

    private val expectedHighDifficulty = listOf(
        "Aperturas",
        "Cruce de Polea Alta",
        "Curl Bayesian en Banco Inclinado",
        "Curl Martillo",
        "Curl de Concentración",
        "Curl de Martillo Cruzado",
        "Curl de Predicador",
        "Elevación Lateral",
        "Extensión de Tríceps en Polea (Pushdown)",
        "Extensión de Tríceps por encima de la Cabeza",
        "Face Pull",
        "Rompecráneos",
        "Vuelos Posteriores",
    )

    private val expectedLowDifficulty = listOf(
        "Hip Thrust",
        "Peso Muerto Rumano",
        "Prensa Inclinada",
        "Press Militar",
        "Press de Banca Inclinado",
        "Press de Banca Plano",
        "Remo T Inclinado",
        "Sentadilla Hack",
    )

    @Test
    fun `every seeded exercise declares a progression difficulty`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue(
                "${exercise.name} sin dificultad de progresión",
                exercise.progressionDifficulty in ProgressionDifficulty.entries,
            )
        }
    }

    @Test
    fun `difficulty distribution splits the catalog into 13 high, 8 low and 16 medium`() {
        val byDifficulty = ExerciseCatalog.ALL.groupingBy { it.progressionDifficulty }.eachCount()

        assertEquals(13, byDifficulty[ProgressionDifficulty.HIGH])
        assertEquals(8, byDifficulty[ProgressionDifficulty.LOW])
        assertEquals(16, byDifficulty[ProgressionDifficulty.MEDIUM])
    }

    @Test
    fun `small zone isolation work is classified as high difficulty`() {
        val actual = ExerciseCatalog.ALL
            .filter { it.progressionDifficulty == ProgressionDifficulty.HIGH }
            .map { it.name }
            .sorted()

        assertEquals(expectedHighDifficulty.sorted(), actual)
    }

    @Test
    fun `heavy multi joint compounds are classified as low difficulty`() {
        val actual = ExerciseCatalog.ALL
            .filter { it.progressionDifficulty == ProgressionDifficulty.LOW }
            .map { it.name }
            .sorted()

        assertEquals(expectedLowDifficulty.sorted(), actual)
    }

    @Test
    fun `the rest of the catalog stays on the default medium difficulty`() {
        val classified = (expectedHighDifficulty + expectedLowDifficulty).toSet()
        val medium = ExerciseCatalog.ALL
            .filter { it.progressionDifficulty == ProgressionDifficulty.MEDIUM }
            .map { it.name }

        medium.forEach { name ->
            assertFalse("$name está clasificado y no debería quedar en Media", name in classified)
        }
        assertEquals(ExerciseCatalog.ALL.size - classified.size, medium.size)
    }

    @Test
    fun `effective thresholds with the default base reflect the seed classification`() {
        val base = PlateauThresholdRule.DEFAULT_BASE_THRESHOLD

        assertEquals(
            10,
            PlateauThresholdRule.effectiveThreshold(base, ExerciseCatalog.byId(10)!!.progressionDifficulty),
        )
        assertEquals(
            5,
            PlateauThresholdRule.effectiveThreshold(base, ExerciseCatalog.byId(17)!!.progressionDifficulty),
        )
        assertEquals(
            8,
            PlateauThresholdRule.effectiveThreshold(base, ExerciseCatalog.byId(25)!!.progressionDifficulty),
        )
    }
}
