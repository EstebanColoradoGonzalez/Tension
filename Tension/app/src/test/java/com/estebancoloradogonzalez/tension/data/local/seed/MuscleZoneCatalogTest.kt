package com.estebancoloradogonzalez.tension.data.local.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La tabla cerrada de CA-41.01, transcrita **desde el texto de la historia** y no desde
 * [MuscleZoneCatalog].
 *
 * Es deliberado que las 33 filas estén escritas dos veces: un id equivocado en el seed no
 * lanza ninguna excepción, produce un catálogo plausible y una métrica silenciosamente
 * falsa. Dos transcripciones independientes de la misma tabla es lo único que lo detecta.
 */
class MuscleZoneCatalogTest {

    /** Las 33 zonas de CA-41.01: nombre, grupo muscular y orden declarado. */
    private val acceptanceTable: List<Triple<String, String, Int>> = listOf(
        Triple("Pectoral Superior", "Pecho", 1),
        Triple("Pectoral Medio", "Pecho", 2),
        Triple("Pectoral Inferior", "Pecho", 3),
        Triple("Pectoral Mayor", "Pecho", 4),
        Triple("Deltoides Anterior", "Hombro", 5),
        Triple("Deltoides Lateral", "Hombro", 6),
        Triple("Deltoides Posterior", "Hombro", 7),
        Triple("Manguito Rotador", "Hombro", 8),
        Triple("Dorsal Ancho", "Espalda", 9),
        Triple("Espalda Alta", "Espalda", 10),
        Triple("Trapecio", "Espalda", 11),
        Triple("Trapecio Superior", "Espalda", 12),
        Triple("Trapecio Inferior", "Espalda", 13),
        Triple("Romboides", "Espalda", 14),
        Triple("Erectores Espinales", "Espalda", 15),
        Triple("Bíceps Braquial", "Bíceps", 16),
        Triple("Bíceps — Cabeza Larga", "Bíceps", 17),
        Triple("Bíceps — Cabeza Corta", "Bíceps", 18),
        Triple("Tríceps Braquial", "Tríceps", 19),
        Triple("Tríceps — Cabeza Larga", "Tríceps", 20),
        Triple("Tríceps — Cabeza Lateral", "Tríceps", 21),
        Triple("Tríceps — Cabeza Medial", "Tríceps", 22),
        Triple("Braquial", "Antebrazo", 23),
        Triple("Braquiorradial", "Antebrazo", 24),
        Triple("Recto Abdominal", "Abdomen", 25),
        Triple("Oblicuos", "Abdomen", 26),
        Triple("Cuádriceps", "Cuádriceps", 27),
        Triple("Isquiotibiales", "Isquiotibiales", 28),
        Triple("Glúteo Mayor", "Glúteos", 29),
        Triple("Aductores", "Aductores", 30),
        Triple("Abductores", "Abductores", 31),
        Triple("Gastrocnemio", "Gemelos", 32),
        Triple("Cuello", "Cuello", 33),
    )

    // ============================================================
    // CA-41.01 — 33 zonas con granularidad anatómica
    // ============================================================

    @Test
    fun `catalog contains exactly 33 muscle zones`() {
        assertEquals(33, MuscleZoneCatalog.ALL.size)
    }

    @Test
    fun `the 33 zones match the acceptance table in name, group and order`() {
        assertEquals(
            acceptanceTable,
            MuscleZoneCatalog.ALL.map { Triple(it.name, it.muscleGroup, it.sortOrder) },
        )
    }

    @Test
    fun `zone ids are unique`() {
        val ids = MuscleZoneCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `zone names are unique`() {
        val names = MuscleZoneCatalog.ALL.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }

    // ============================================================
    // CA-41.01 — los 14 grupos musculares no cambian
    // ============================================================

    @Test
    fun `the fourteen muscle groups are unchanged`() {
        assertEquals(
            listOf(
                "Pecho",
                "Hombro",
                "Espalda",
                "Bíceps",
                "Tríceps",
                "Antebrazo",
                "Abdomen",
                "Cuádriceps",
                "Isquiotibiales",
                "Glúteos",
                "Aductores",
                "Abductores",
                "Gemelos",
                "Cuello",
            ),
            MuscleZoneCatalog.ALL.map { it.muscleGroup }.distinct(),
        )
    }

    @Test
    fun `every zone belongs to one of the declared muscle groups`() {
        MuscleZoneCatalog.ALL.forEach { zone ->
            assertTrue(
                "${zone.name} cuelga de un grupo no declarado: ${zone.muscleGroup}",
                zone.muscleGroup in MuscleZoneCatalog.MUSCLE_GROUPS,
            )
        }
    }

    @Test
    fun `zones of the same group are contiguous in declared order`() {
        // Agrupar por grupo muscular es lo que hace el selector; si un grupo apareciera
        // partido en dos tramos, su encabezado se pintaría dos veces.
        val groupsInOrder = MuscleZoneCatalog.ALL.map { it.muscleGroup }
        val firstAppearance = groupsInOrder.distinct()
        assertEquals(firstAppearance.size, groupsInOrder.zipWithNext().count { it.first != it.second } + 1)
    }

    @Test
    fun `zone counts per group match the acceptance table`() {
        assertEquals(
            mapOf(
                "Pecho" to 4,
                "Hombro" to 4,
                "Espalda" to 7,
                "Bíceps" to 3,
                "Tríceps" to 4,
                "Antebrazo" to 2,
                "Abdomen" to 2,
                "Cuádriceps" to 1,
                "Isquiotibiales" to 1,
                "Glúteos" to 1,
                "Aductores" to 1,
                "Abductores" to 1,
                "Gemelos" to 1,
                "Cuello" to 1,
            ),
            MuscleZoneCatalog.ALL.groupingBy { it.muscleGroup }.eachCount(),
        )
    }

    // ============================================================
    // CA-41.01 — las tres retiradas y la ausencia de Core
    // ============================================================

    @Test
    fun `the three retired zones are gone from the catalog`() {
        val names = MuscleZoneCatalog.ALL.map { it.name }
        listOf("Hombro", "Antebrazo", "Espalda Media").forEach { retired ->
            assertTrue("«$retired» debía haberse retirado", retired !in names)
        }
    }

    @Test
    fun `retired ids are not reused`() {
        MuscleZoneCatalog.RETIRED_IDS.forEach { id ->
            assertNull("El id retirado $id se reutilizó", MuscleZoneCatalog.byId(id))
        }
    }

    @Test
    fun `retired ids are exactly the three of the acceptance criterion`() {
        assertEquals(setOf(4L, 7L, 19L), MuscleZoneCatalog.RETIRED_IDS)
    }

    @Test
    fun `core is not a muscle zone`() {
        // No es un músculo sino una región: los anti-rotación se catalogan por Oblicuos y
        // Recto Abdominal.
        assertTrue(MuscleZoneCatalog.ALL.none { it.name.equals("Core", ignoreCase = true) })
    }

    // ============================================================
    // CA-41.01 — las zonas conservadas sin ejercicio
    // ============================================================

    @Test
    fun `abductores and cuello are kept even without a seeded exercise`() {
        val used = ExerciseCatalog.ALL.flatMap { it.allMuscleZoneIds }.toSet()
        listOf(MuscleZoneCatalog.ABDUCTORES, MuscleZoneCatalog.CUELLO).forEach { id ->
            assertNotNull("La zona $id debe existir en el catálogo", MuscleZoneCatalog.byId(id))
            assertTrue("La zona $id no debería tener ejercicio semilla", id !in used)
        }
    }

    @Test
    fun `every other zone is used by at least one seeded exercise`() {
        val used = ExerciseCatalog.ALL.flatMap { it.allMuscleZoneIds }.toSet()
        val unused = MuscleZoneCatalog.ALL
            .filter { it.id !in used }
            .map { it.name }
        assertEquals(listOf("Abductores", "Cuello"), unused)
    }

    // ============================================================
    // CA-41.01 — los identificadores conservados (lección de HU-29)
    // ============================================================

    @Test
    fun `the seventeen surviving zones keep their identifier`() {
        // Ocho conservan nombre e id; nueve se renombraron conservando el id. Un renombrado
        // que perdiera el id perdería con él la imagen, la clasificación y el historial.
        assertEquals(
            mapOf(
                1L to "Pectoral Medio",
                2L to "Pectoral Superior",
                3L to "Pectoral Inferior",
                5L to "Dorsal Ancho",
                6L to "Recto Abdominal",
                8L to "Tríceps Braquial",
                9L to "Bíceps Braquial",
                10L to "Cuádriceps",
                11L to "Isquiotibiales",
                12L to "Aductores",
                13L to "Abductores",
                14L to "Gastrocnemio",
                15L to "Glúteo Mayor",
                16L to "Espalda Alta",
                17L to "Trapecio",
                18L to "Erectores Espinales",
                20L to "Cuello",
            ),
            MuscleZoneCatalog.ALL.filter { it.id <= 20L }.associate { it.id to it.name },
        )
    }

    @Test
    fun `the sixteen new zones occupy ids 21 to 36`() {
        assertEquals(
            (21L..36L).toList(),
            MuscleZoneCatalog.ALL.map { it.id }.filter { it > 20L }.sorted(),
        )
    }

    // ============================================================
    // CA-41.02 — el orden que el selector presenta
    // ============================================================

    @Test
    fun `sort order is dense and starts at one`() {
        assertEquals(
            (1..33).toList(),
            MuscleZoneCatalog.ALL.map { it.sortOrder }.sorted(),
        )
    }

    @Test
    fun `declared order is not alphabetical`() {
        // Ordenar por nombre daría *Pectoral Inferior, Mayor, Medio, Superior*. El orden
        // del catálogo es anatómico, y esa diferencia es la razón de que sort_order exista.
        val declared = MuscleZoneCatalog.ALL.map { it.name }
        assertTrue(declared != declared.sorted())
    }

    @Test
    fun `byId resolves every zone of the catalog`() {
        MuscleZoneCatalog.ALL.forEach { zone ->
            assertEquals(zone, MuscleZoneCatalog.byId(zone.id))
        }
    }
}
