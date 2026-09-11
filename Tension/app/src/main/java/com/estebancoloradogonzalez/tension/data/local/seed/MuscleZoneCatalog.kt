package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedMuscleZone

/**
 * Catálogo de zonas musculares precargado en instalación fresca: **33 zonas con
 * granularidad anatómica** repartidas en los **14 grupos musculares** de siempre (HU-41).
 *
 * Sustituye a las 20 zonas anteriores, cuya granularidad no alcanzaba para describir lo
 * que un ejercicio hace: `Elevación Lateral` y `Press Militar` figuraban ambos como
 * «Hombro» cuando uno aísla el deltoides lateral y el otro el anterior. Tres zonas se
 * **retiran** por quedar cubiertas por otras más específicas —`Hombro`, `Antebrazo` y
 * `Espalda Media`— y sus identificadores (4, 7, 19) **quedan libres y no se reutilizan**:
 * reasignarlos haría que cualquier referencia antigua apuntara en silencio a otra cosa.
 *
 * Las 17 zonas que sobreviven **conservan su identificador**, renombradas o no, por la
 * regla que HU-29 fijó para los renombrados. Las 16 nuevas ocupan 21-36.
 *
 * `Abductores` y `Cuello` se **conservan sin ejercicio semilla** para que el ejecutante
 * pueda catalogar ejercicios propios de esas zonas. `Core` **no** se crea: no es un
 * músculo sino una región, y los movimientos anti-rotación se catalogan por `Oblicuos` y
 * `Recto Abdominal`.
 *
 * [SeedMuscleZone.sortOrder] es la posición en [ALL] y gobierna el orden de presentación:
 * los 14 grupos en orden anatómico y, dentro de cada uno, las zonas de mayor a menor. La
 * tabla es cerrada y sembrada —no existe interfaz para crear zonas— así que ordenar por
 * `sort_order` es ordenar por el criterio del catálogo y no por un accidente alfabético.
 */
object MuscleZoneCatalog {

    // ===== Pecho =====
    const val PECTORAL_SUPERIOR = 2L
    const val PECTORAL_MEDIO = 1L
    const val PECTORAL_INFERIOR = 3L
    const val PECTORAL_MAYOR = 21L

    // ===== Hombro =====
    const val DELTOIDES_ANTERIOR = 22L
    const val DELTOIDES_LATERAL = 23L
    const val DELTOIDES_POSTERIOR = 24L
    const val MANGUITO_ROTADOR = 25L

    // ===== Espalda =====
    const val DORSAL_ANCHO = 5L
    const val ESPALDA_ALTA = 16L
    const val TRAPECIO = 17L
    const val TRAPECIO_SUPERIOR = 26L
    const val TRAPECIO_INFERIOR = 27L
    const val ROMBOIDES = 28L
    const val ERECTORES_ESPINALES = 18L

    // ===== Bíceps =====
    const val BICEPS_BRAQUIAL = 9L
    const val BICEPS_CABEZA_LARGA = 29L
    const val BICEPS_CABEZA_CORTA = 30L

    // ===== Tríceps =====
    const val TRICEPS_BRAQUIAL = 8L
    const val TRICEPS_CABEZA_LARGA = 31L
    const val TRICEPS_CABEZA_LATERAL = 32L
    const val TRICEPS_CABEZA_MEDIAL = 33L

    // ===== Antebrazo =====
    const val BRAQUIAL = 34L
    const val BRAQUIORRADIAL = 35L

    // ===== Abdomen =====
    const val RECTO_ABDOMINAL = 6L
    const val OBLICUOS = 36L

    // ===== Un grupo, una zona =====
    const val CUADRICEPS = 10L
    const val ISQUIOTIBIALES = 11L
    const val GLUTEO_MAYOR = 15L
    const val ADUCTORES = 12L
    const val ABDUCTORES = 13L
    const val GASTROCNEMIO = 14L
    const val CUELLO = 20L

    /**
     * Identificadores de las zonas retiradas en HU-41. **No se reutilizan.**
     *
     * `Hombro` quedó cubierta por los tres deltoides y el manguito rotador, `Antebrazo`
     * por braquial y braquiorradial, y `Espalda Media` por trapecio, romboides y espalda
     * alta.
     */
    val RETIRED_IDS: Set<Long> = setOf(4L, 7L, 19L)

    /** Los 14 grupos musculares, en el orden en que el selector los presenta. */
    val MUSCLE_GROUPS: List<String> = listOf(
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
    )

    val ALL: List<SeedMuscleZone> = listOf(
        // ===== Pecho =====
        zone(PECTORAL_SUPERIOR, "Pectoral Superior", "Pecho", 1),
        zone(PECTORAL_MEDIO, "Pectoral Medio", "Pecho", 2),
        zone(PECTORAL_INFERIOR, "Pectoral Inferior", "Pecho", 3),
        zone(PECTORAL_MAYOR, "Pectoral Mayor", "Pecho", 4),

        // ===== Hombro ===== (sustituyen a la zona «Hombro», retirada)
        zone(DELTOIDES_ANTERIOR, "Deltoides Anterior", "Hombro", 5),
        zone(DELTOIDES_LATERAL, "Deltoides Lateral", "Hombro", 6),
        zone(DELTOIDES_POSTERIOR, "Deltoides Posterior", "Hombro", 7),
        zone(MANGUITO_ROTADOR, "Manguito Rotador", "Hombro", 8),

        // ===== Espalda ===== (trapecio, romboides y espalda alta cubren «Espalda Media»)
        zone(DORSAL_ANCHO, "Dorsal Ancho", "Espalda", 9),
        zone(ESPALDA_ALTA, "Espalda Alta", "Espalda", 10),
        zone(TRAPECIO, "Trapecio", "Espalda", 11),
        zone(TRAPECIO_SUPERIOR, "Trapecio Superior", "Espalda", 12),
        zone(TRAPECIO_INFERIOR, "Trapecio Inferior", "Espalda", 13),
        zone(ROMBOIDES, "Romboides", "Espalda", 14),
        zone(ERECTORES_ESPINALES, "Erectores Espinales", "Espalda", 15),

        // ===== Bíceps =====
        zone(BICEPS_BRAQUIAL, "Bíceps Braquial", "Bíceps", 16),
        zone(BICEPS_CABEZA_LARGA, "Bíceps — Cabeza Larga", "Bíceps", 17),
        zone(BICEPS_CABEZA_CORTA, "Bíceps — Cabeza Corta", "Bíceps", 18),

        // ===== Tríceps =====
        zone(TRICEPS_BRAQUIAL, "Tríceps Braquial", "Tríceps", 19),
        zone(TRICEPS_CABEZA_LARGA, "Tríceps — Cabeza Larga", "Tríceps", 20),
        zone(TRICEPS_CABEZA_LATERAL, "Tríceps — Cabeza Lateral", "Tríceps", 21),
        zone(TRICEPS_CABEZA_MEDIAL, "Tríceps — Cabeza Medial", "Tríceps", 22),

        // ===== Antebrazo ===== (sustituyen a la zona «Antebrazo», retirada)
        zone(BRAQUIAL, "Braquial", "Antebrazo", 23),
        zone(BRAQUIORRADIAL, "Braquiorradial", "Antebrazo", 24),

        // ===== Abdomen =====
        zone(RECTO_ABDOMINAL, "Recto Abdominal", "Abdomen", 25),
        zone(OBLICUOS, "Oblicuos", "Abdomen", 26),

        // ===== Un grupo, una zona =====
        zone(CUADRICEPS, "Cuádriceps", "Cuádriceps", 27),
        zone(ISQUIOTIBIALES, "Isquiotibiales", "Isquiotibiales", 28),
        zone(GLUTEO_MAYOR, "Glúteo Mayor", "Glúteos", 29),
        zone(ADUCTORES, "Aductores", "Aductores", 30),
        // Sin ejercicio semilla: disponible para los ejercicios que cree el ejecutante.
        zone(ABDUCTORES, "Abductores", "Abductores", 31),
        zone(GASTROCNEMIO, "Gastrocnemio", "Gemelos", 32),
        // Sin ejercicio semilla, por la misma razón que Abductores.
        zone(CUELLO, "Cuello", "Cuello", 33),
    )

    fun byId(id: Long): SeedMuscleZone? = ALL.firstOrNull { it.id == id }

    private fun zone(id: Long, name: String, muscleGroup: String, sortOrder: Int) =
        SeedMuscleZone(id = id, name = name, muscleGroup = muscleGroup, sortOrder = sortOrder)
}
