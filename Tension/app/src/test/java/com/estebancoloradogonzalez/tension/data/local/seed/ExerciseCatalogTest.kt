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

    private fun equipmentNamesOf(id: Long): List<String> =
        ExerciseCatalog.byId(id)!!.equipmentTypeIds.map { EquipmentCatalog.byId(it)!!.name }

    private fun zoneNamesOf(zoneIds: List<Long>): List<String> =
        zoneIds.map { MuscleZoneCatalog.byId(it)!!.name }

    @Test
    fun `catalog contains exactly 38 exercises`() {
        assertEquals(38, ExerciseCatalog.ALL.size)
    }

    @Test
    fun `exercise ids are unique`() {
        val ids = ExerciseCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    // CA-39.02 — el nombre es único por sí solo: el implemento no es parte de la identidad

    @Test
    fun `exercise names are unique on their own`() {
        val names = ExerciseCatalog.ALL.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }

    // ============================================================
    // CA-41.02 / CA-41.03 — Jerarquía principal / secundaria
    // ============================================================

    @Test
    fun `exercise muscle zone relations total 87`() {
        assertEquals(87, ExerciseCatalog.ALL.sumOf { it.allMuscleZoneIds.size })
    }

    @Test
    fun `relations split into 52 primary and 35 secondary`() {
        assertEquals(52, ExerciseCatalog.ALL.sumOf { it.primaryMuscleZoneIds.size })
        assertEquals(35, ExerciseCatalog.ALL.sumOf { it.secondaryMuscleZoneIds.size })
    }

    @Test
    fun `no exercise is left without a primary muscle zone`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue(
                "${exercise.name} sin zona principal",
                exercise.primaryMuscleZoneIds.isNotEmpty(),
            )
        }
    }

    @Test
    fun `no zone is primary and secondary of the same exercise`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            val shared = exercise.primaryMuscleZoneIds
                .intersect(exercise.secondaryMuscleZoneIds.toSet())
            assertTrue(
                "${exercise.name} repite zonas entre principal y secundaria: $shared",
                shared.isEmpty(),
            )
        }
    }

    @Test
    fun `every declared muscle zone exists in the catalog and is not duplicated`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            exercise.allMuscleZoneIds.forEach { zoneId ->
                assertNotNull(
                    "${exercise.name} declara una zona inexistente: $zoneId",
                    MuscleZoneCatalog.byId(zoneId),
                )
            }
            assertEquals(
                "${exercise.name} con zonas duplicadas",
                exercise.allMuscleZoneIds.size,
                exercise.allMuscleZoneIds.distinct().size,
            )
        }
    }

    @Test
    fun `no exercise declares a retired muscle zone`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            exercise.allMuscleZoneIds.forEach { zoneId ->
                assertFalse(
                    "${exercise.name} usa la zona retirada $zoneId",
                    zoneId in MuscleZoneCatalog.RETIRED_IDS,
                )
            }
        }
    }

    /**
     * La tabla de CA-41.03 transcrita desde el texto de la historia, **por nombre y no por
     * id**.
     *
     * Por nombre a propósito: un id equivocado en el seed y el mismo id equivocado aquí se
     * cancelarían sin que nadie lo notara. Los nombres los fija [MuscleZoneCatalog], que
     * tiene su propia prueba contra la tabla de CA-41.01.
     */
    @Test
    fun `the 38 exercises are classified exactly as the acceptance table declares`() {
        val expected = mapOf(
            "Aductores" to (listOf("Aductores") to emptyList<String>()),
            "Cruce de Polea Alta" to (listOf("Pectoral Inferior") to listOf("Deltoides Anterior")),
            "Crunch Abdominal" to (listOf("Recto Abdominal") to emptyList()),
            "Curl Bayesian en Banco Inclinado" to (listOf("Bíceps — Cabeza Larga") to emptyList()),
            "Curl de Concentración" to (listOf("Bíceps Braquial") to emptyList()),
            "Curl de Isquiotibiales Sentado" to (listOf("Isquiotibiales") to emptyList()),
            "Curl de Martillo Cruzado" to
                (listOf("Braquial", "Braquiorradial") to listOf("Bíceps Braquial")),
            "Curl de Predicador" to (listOf("Bíceps — Cabeza Corta") to emptyList()),
            "Elevación de Pantorrilla de Pie" to (listOf("Gastrocnemio") to emptyList()),
            "Elevación Lateral" to (listOf("Deltoides Lateral") to emptyList()),
            "Extensión de Cuádriceps" to (listOf("Cuádriceps") to emptyList()),
            "Extensión de Tríceps (Pushdown)" to
                (listOf("Tríceps — Cabeza Lateral", "Tríceps — Cabeza Medial") to emptyList()),
            "Extensión de Tríceps sobre Cabeza" to
                (listOf("Tríceps — Cabeza Larga") to emptyList()),
            "Face Pull" to (listOf("Deltoides Posterior", "Trapecio") to listOf("Manguito Rotador")),
            "Hip Thrust" to (listOf("Glúteo Mayor") to listOf("Isquiotibiales", "Cuádriceps")),
            "Peso Muerto Rumano" to
                (listOf("Isquiotibiales", "Glúteo Mayor") to listOf("Erectores Espinales")),
            "Prensa Inclinada" to
                (listOf("Cuádriceps", "Glúteo Mayor") to listOf("Isquiotibiales")),
            "Press de Banca Inclinado" to
                (listOf("Pectoral Superior") to listOf("Deltoides Anterior", "Tríceps Braquial")),
            "Press de Banca Plano" to
                (listOf("Pectoral Medio") to listOf("Deltoides Anterior", "Tríceps Braquial")),
            "Press Pallof" to (listOf("Oblicuos") to listOf("Recto Abdominal")),
            "Remo T Inclinado" to
                (listOf("Dorsal Ancho", "Trapecio") to listOf("Bíceps Braquial", "Romboides")),
            "Sentadilla Búlgara" to (listOf("Cuádriceps", "Glúteo Mayor") to emptyList()),
            "Sentadilla Sumo" to (listOf("Glúteo Mayor", "Cuádriceps") to listOf("Aductores")),
            "Sentadilla Hack" to (listOf("Cuádriceps") to listOf("Glúteo Mayor")),
            "Jalón al Pecho" to
                (listOf("Dorsal Ancho") to listOf("Bíceps Braquial", "Espalda Alta")),
            "Vuelos Posteriores (Pájaros)" to
                (listOf("Deltoides Posterior") to listOf("Trapecio", "Romboides")),
            "Remo al Mentón" to
                (listOf("Deltoides Lateral", "Trapecio Superior") to listOf("Bíceps Braquial")),
            "Aperturas" to (listOf("Pectoral Mayor") to listOf("Deltoides Anterior")),
            "Pull-Over" to (listOf("Dorsal Ancho") to listOf("Pectoral Inferior")),
            "Curl Martillo" to
                (listOf("Braquial", "Braquiorradial") to listOf("Bíceps Braquial")),
            "Rompecráneos" to (listOf("Tríceps Braquial") to emptyList()),
            "Remo Horizontal" to
                (listOf("Dorsal Ancho") to listOf("Trapecio", "Romboides", "Bíceps Braquial")),
            "Zancadas (Lunges)" to (listOf("Cuádriceps", "Glúteo Mayor") to emptyList()),
            "Press Militar" to
                (listOf("Deltoides Anterior", "Deltoides Lateral") to listOf("Tríceps Braquial")),
            "Dominadas" to
                (listOf("Dorsal Ancho") to listOf("Bíceps Braquial", "Trapecio Inferior")),
            "Remo Unilateral Polea Baja" to
                (listOf("Dorsal Ancho") to listOf("Bíceps Braquial", "Romboides")),
            "Remo Unilateral Polea Alta" to
                (listOf("Dorsal Ancho", "Espalda Alta") to listOf("Bíceps Braquial")),
            // Añadido por HU-41 para el cuarto puesto del viernes.
            "Trapecios con Apoyo en Banco Inclinado" to
                (
                    listOf("Trapecio", "Trapecio Inferior") to
                        listOf("Romboides", "Deltoides Posterior")
                    ),
        )

        assertEquals(38, expected.size)
        assertEquals(
            expected,
            ExerciseCatalog.ALL.associate { exercise ->
                exercise.name to (
                    zoneNamesOf(exercise.primaryMuscleZoneIds) to
                        zoneNamesOf(exercise.secondaryMuscleZoneIds)
                    )
            },
        )
    }

    @Test
    fun `every exercise declares a media resource`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue("${exercise.name} sin media_resource", exercise.mediaResource.isNotBlank())
        }
    }

    // ============================================================
    // CA-39.01 — Catálogo de equipamiento normalizado
    // ============================================================

    @Test
    fun `equipment catalog contains exactly 15 atomic types in declared order`() {
        assertEquals(
            listOf(
                "Máquina",
                "Máquina Smith",
                "Polea",
                "Barra",
                "Barra Fija",
                "Mancuerna",
                "Pesa Rusa",
                "Banda Elástica",
                "Peso Corporal",
                "Peso Añadido",
                "Barra EZ",
                "TRX/Suspensión",
                "Balón Medicinal",
                "Rodillo de Abdomen",
                "Paralelas/Dip Station",
            ),
            EquipmentCatalog.ALL.map { it.name },
        )
    }

    /** El identificador encoda el orden declarado: es el que la interfaz presenta. */
    @Test
    fun `equipment ids are the declared positions, one to fifteen`() {
        assertEquals((1L..15L).toList(), EquipmentCatalog.ALL.map { it.id })
    }

    @Test
    fun `no equipment type expresses a disjunction`() {
        EquipmentCatalog.ALL.forEach { type ->
            assertFalse(
                "${type.name} expresa una disyunción, no un implemento",
                type.name.contains(" o "),
            )
        }
    }

    @Test
    fun `the ten composite or duplicated types are gone`() {
        val retired = listOf(
            "Mancuernas",
            "Pesa",
            "Máquina Multiestación",
            "Polea con Cuerda",
            "Polea con Barra en V",
            "Polea con Cuerda o Polea con Barra en V",
            "Mancuerna o Polea",
            "Mancuerna o Polea o Barra",
            "Barra o Mancuernas",
            "Mancuernas o Polea",
            "Mancuerna o Pesa Rusa",
            "Barra de Pesas",
            "Kettlebell",
            "Cuerpo",
        )
        val current = EquipmentCatalog.ALL.map { it.name }
        retired.forEach { name ->
            assertFalse("$name sigue en el catálogo", name in current)
        }
    }

    @Test
    fun `equipment names are unique`() {
        val names = EquipmentCatalog.ALL.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }

    /** `Máquina de Remo` y `Máquina Contractor` no son tipos propios: ambos son `Máquina`. */
    @Test
    fun `machine variants are not their own types`() {
        val current = EquipmentCatalog.ALL.map { it.name }
        assertFalse("Máquina de Remo" in current)
        assertFalse("Máquina Contractor" in current)
    }

    // ============================================================
    // CA-39.02 / CA-39.06 — Un ejercicio, varios implementos
    // ============================================================

    @Test
    fun `exercise equipment relations total 100`() {
        val relations = ExerciseCatalog.ALL.sumOf { it.equipmentTypeIds.size }
        assertEquals(100, relations)
    }

    @Test
    fun `no exercise is left without equipment`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertTrue(
                "${exercise.name} sin equipamiento admitido",
                exercise.equipmentTypeIds.isNotEmpty(),
            )
        }
    }

    @Test
    fun `every declared equipment exists in the catalog and is not duplicated`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            exercise.equipmentTypeIds.forEach { id ->
                assertNotNull(
                    "${exercise.name} declara el equipamiento inexistente $id",
                    EquipmentCatalog.byId(id),
                )
            }
            assertEquals(
                "${exercise.name} con equipamientos duplicados",
                exercise.equipmentTypeIds.size,
                exercise.equipmentTypeIds.distinct().size,
            )
        }
    }

    /**
     * La tabla completa y cerrada de CA-39.06.
     *
     * Declarada como tabla y no como delta narrado: es la lección de HU-29 — una
     * recategorización enunciada por diferencias deja ambigüedad en el sembrado.
     */
    @Test
    fun `the 37 exercises declare exactly the equipment of the acceptance table`() {
        val expected = mapOf(
            1L to listOf("Máquina", "Polea", "Banda Elástica"),
            2L to listOf("Polea"),
            3L to listOf("Peso Corporal", "Polea", "Máquina"),
            4L to listOf("Polea", "Mancuerna"),
            5L to listOf("Mancuerna", "Polea"),
            6L to listOf("Máquina"),
            7L to listOf("Mancuerna", "Polea"),
            8L to listOf("Barra", "Mancuerna", "Máquina", "Polea"),
            9L to listOf("Máquina", "Máquina Smith"),
            10L to listOf("Mancuerna", "Polea", "Máquina"),
            11L to listOf("Máquina"),
            12L to listOf("Polea"),
            13L to listOf("Mancuerna", "Barra", "Polea"),
            14L to listOf("Polea", "Banda Elástica"),
            15L to listOf("Barra", "Máquina", "Mancuerna", "Máquina Smith"),
            16L to listOf("Barra", "Mancuerna", "Máquina Smith"),
            17L to listOf("Máquina"),
            18L to listOf("Barra", "Mancuerna", "Máquina", "Máquina Smith"),
            19L to listOf("Barra", "Mancuerna", "Máquina", "Máquina Smith"),
            20L to listOf("Polea", "Banda Elástica"),
            21L to listOf("Barra", "Máquina"),
            22L to listOf("Peso Corporal", "Mancuerna", "Barra", "Máquina Smith"),
            23L to listOf("Mancuerna", "Pesa Rusa", "Barra", "Polea"),
            24L to listOf("Máquina", "Barra"),
            25L to listOf("Polea", "Máquina"),
            26L to listOf("Mancuerna", "Polea", "Máquina"),
            27L to listOf("Barra", "Polea", "Mancuerna"),
            28L to listOf("Mancuerna", "Polea", "Máquina"),
            29L to listOf("Mancuerna", "Polea", "Barra", "Máquina"),
            30L to listOf("Mancuerna", "Polea"),
            31L to listOf("Barra", "Mancuerna", "Polea"),
            32L to listOf("Barra", "Mancuerna", "Polea", "Máquina"),
            33L to listOf("Peso Corporal", "Mancuerna", "Barra", "Máquina Smith"),
            34L to listOf("Barra", "Mancuerna", "Máquina", "Máquina Smith"),
            35L to listOf("Barra Fija", "Máquina", "Peso Añadido"),
            36L to listOf("Polea"),
            37L to listOf("Polea"),
        )

        assertEquals(37, expected.size)
        expected.forEach { (exerciseId, equipmentNames) ->
            assertEquals(
                "Equipamiento incorrecto en ${ExerciseCatalog.byId(exerciseId)!!.name}",
                equipmentNames,
                equipmentNamesOf(exerciseId),
            )
        }
    }

    /** Cinco tipos nacen sin ejercicio semilla: disponibles a futuro (CA-39.01). */
    @Test
    fun `five equipment types have no seeded exercise`() {
        val used = ExerciseCatalog.ALL.flatMap { it.equipmentTypeIds }.toSet()
        val unused = EquipmentCatalog.ALL.filterNot { it.id in used }.map { it.name }
        assertEquals(
            listOf(
                "Barra EZ",
                "TRX/Suspensión",
                "Balón Medicinal",
                "Rodillo de Abdomen",
                "Paralelas/Dip Station",
            ),
            unused,
        )
    }

    // ============================================================
    // CA-39.07 — Renombrado de ocho ejercicios
    // ============================================================

    @Test
    fun `the eight renamed exercises carry their new names`() {
        val renamed = mapOf(
            9L to "Elevación de Pantorrilla de Pie",
            12L to "Extensión de Tríceps (Pushdown)",
            13L to "Extensión de Tríceps sobre Cabeza",
            23L to "Sentadilla Sumo",
            26L to "Vuelos Posteriores (Pájaros)",
            33L to "Zancadas (Lunges)",
            36L to "Remo Unilateral Polea Baja",
            37L to "Remo Unilateral Polea Alta",
        )
        renamed.forEach { (id, name) ->
            assertEquals(name, ExerciseCatalog.byId(id)!!.name)
        }
    }

    @Test
    fun `the former names are gone from the catalog`() {
        val formerNames = listOf(
            "Elevación de Pantorrilla en Máquina de Pie",
            "Extensión de Tríceps en Polea (Pushdown)",
            "Extensión de Tríceps por encima de la Cabeza",
            "Sentadilla de Zumo",
            "Vuelos Posteriores",
            "Zancadas",
            "Remo Unilateral en Polea Baja",
            "Remo Unilateral en Polea Alta",
        )
        formerNames.forEach { name ->
            assertNull(
                "El nombre anterior $name sigue en el catálogo",
                ExerciseCatalog.ALL.firstOrNull { it.name == name },
            )
        }
    }

    /**
     * El asset **no** se renombra: el recurso visual se conserva, como en HU-29.
     *
     * La clasificación muscular ya no se comprueba aquí: HU-41 la reescribió entera y la
     * tabla cerrada de CA-41.03 la verifica ejercicio a ejercicio. Lo que este caso
     * protege es lo que un renombrado no puede tocar — el identificador y la imagen.
     */
    @Test
    fun `each renamed exercise keeps its id and its media resource`() {
        val preserved = mapOf(
            9L to "elevacion_de_pantorrilla_en_maquina_de_pie_maquina",
            12L to "extension_de_triceps_en_polea_pushdown_polea_con_cuerda",
            13L to "extension_de_triceps_por_encima_de_la_cabeza_mancuernas",
            23L to "sentadilla_de_zumo_mancuerna",
            26L to "vuelos_posteriores_mancuernas",
            33L to "zancadas_mancuernas",
            36L to "remo_unilateral_en_polea_baja_polea",
            37L to "remo_unilateral_en_polea_alta_polea",
        )
        preserved.forEach { (id, mediaResource) ->
            assertEquals(mediaResource, ExerciseCatalog.byId(id)!!.mediaResource)
        }
    }

    @Test
    fun `the remaining thirty exercises keep their names`() {
        val renamedIds = setOf(9L, 12L, 13L, 23L, 26L, 33L, 36L, 37L)
        assertEquals(30, ExerciseCatalog.ALL.count { it.id !in renamedIds })
    }

    @Test
    fun `exercise names do not embed their equipment variant`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            assertFalse(
                "${exercise.name} incorpora la mención del equipamiento",
                exercise.name.contains(" o "),
            )
        }
    }

    // CA-29.03 — Renombrado de Tirón de Dorsales

    @Test
    fun `jalon al pecho keeps the identity of tiron de dorsales`() {
        val exercise = ExerciseCatalog.byId(25)
        assertNotNull(exercise)
        assertEquals("Jalón al Pecho", exercise!!.name)
        assertEquals("tiron_de_dorsales_polea", exercise.mediaResource)
        assertEquals(listOf(MuscleZoneCatalog.DORSAL_ANCHO), exercise.primaryMuscleZoneIds)
    }

    @Test
    fun `tiron de dorsales is not duplicated under its former name`() {
        assertNull(ExerciseCatalog.ALL.firstOrNull { it.name == "Tirón de Dorsales" })
        assertEquals(1, ExerciseCatalog.ALL.count { it.mediaResource == "tiron_de_dorsales_polea" })
    }

    // CA-29.02 / CA-29.06 — Ejercicios nuevos

    @Test
    fun `press militar is registered with its four implements and deltoid zones`() {
        val exercise = ExerciseCatalog.byId(34)!!
        assertEquals("Press Militar", exercise.name)
        assertEquals(
            listOf("Barra", "Mancuerna", "Máquina", "Máquina Smith"),
            equipmentNamesOf(34),
        )
        // HU-41 reemplazó la zona genérica «Hombro» por los deltoides que ejecutan el
        // press, con el tríceps asistiendo.
        assertEquals(
            listOf(MuscleZoneCatalog.DELTOIDES_ANTERIOR, MuscleZoneCatalog.DELTOIDES_LATERAL),
            exercise.primaryMuscleZoneIds,
        )
        assertEquals(listOf(MuscleZoneCatalog.TRICEPS_BRAQUIAL), exercise.secondaryMuscleZoneIds)
        assertEquals("press_militar_mancuernas", exercise.mediaResource)
        assertFalse(exercise.isBodyweight)
    }

    // CA-39.05 / CA-39.06 — Dominadas

    @Test
    fun `dominadas keeps its bodyweight mark and offers peso anadido`() {
        val exercise = ExerciseCatalog.byId(35)!!
        assertEquals("Dominadas", exercise.name)
        assertEquals(listOf("Barra Fija", "Máquina", "Peso Añadido"), equipmentNamesOf(35))
        assertEquals(listOf(MuscleZoneCatalog.DORSAL_ANCHO), exercise.primaryMuscleZoneIds)
        assertEquals("dominadas_barra_fija", exercise.mediaResource)
        assertTrue(exercise.isBodyweight)
    }

    @Test
    fun `dominadas is the only bodyweight exercise of the seed`() {
        val bodyweight = ExerciseCatalog.ALL.filter { it.isBodyweight }.map { it.name }
        assertEquals(listOf("Dominadas"), bodyweight)
    }

    /**
     * `Peso Añadido` existe para un solo ejercicio, y es el único de peso corporal.
     *
     * Que ningún otro lo declare no es casualidad: es lastre sobre el propio cuerpo, y sin
     * la marca de peso corporal el implemento no significa nada.
     */
    @Test
    fun `peso anadido is only admitted by the bodyweight exercise`() {
        val withAddedWeight = ExerciseCatalog.ALL
            .filter { EquipmentCatalog.PESO_ANADIDO in it.equipmentTypeIds }
        assertEquals(listOf("Dominadas"), withAddedWeight.map { it.name })
        assertTrue(withAddedWeight.all { it.isBodyweight })
    }

    @Test
    fun `no seeded exercise is isometric or to technical failure`() {
        assertTrue(ExerciseCatalog.ALL.none { it.isIsometric })
        assertTrue(ExerciseCatalog.ALL.none { it.isToTechnicalFailure })
    }

    // CA-41.03 — Auditoría de catalogación muscular

    /**
     * Recatalogado dos veces, y esta prueba registra el porqué de la segunda.
     *
     * HU-29 lo dejó en «Espalda Alta» porque el catálogo de entonces no tenía con qué
     * distinguir el deltoides lateral del trapecio superior: la única alternativa era la
     * pareja genérica «Hombro» + «Trapecio», que decía menos. Con las 33 zonas de HU-41 sí
     * la hay, y el movimiento vuelve a lo que ejecuta.
     */
    @Test
    fun `remo al menton is recatalogued onto the anatomical zones`() {
        val exercise = ExerciseCatalog.byId(27)!!
        assertEquals("Remo al Mentón", exercise.name)
        assertEquals(
            listOf(MuscleZoneCatalog.DELTOIDES_LATERAL, MuscleZoneCatalog.TRAPECIO_SUPERIOR),
            exercise.primaryMuscleZoneIds,
        )
        assertEquals(listOf(MuscleZoneCatalog.BICEPS_BRAQUIAL), exercise.secondaryMuscleZoneIds)
        // La zona genérica que HU-29 le había dado ya no le corresponde.
        assertFalse(MuscleZoneCatalog.ESPALDA_ALTA in exercise.allMuscleZoneIds)
    }

    // CA-29.07 — Preservación del diccionario

    // CA-41.03 / D13 — El ejercicio que HU-41 añade

    @Test
    fun `trapecios con apoyo is the exercise added by this story`() {
        val exercise = ExerciseCatalog.byId(38)!!
        assertEquals("Trapecios con Apoyo en Banco Inclinado", exercise.name)
        assertEquals(listOf("Mancuerna", "Barra", "Máquina Smith"), equipmentNamesOf(38))
        assertEquals(ProgressionDifficulty.MEDIUM, exercise.progressionDifficulty)
        assertFalse(exercise.isBodyweight)
        assertFalse(exercise.isIsometric)
        // El asset llegó sin el «en» que el nombre sí lleva, y manda el archivo: el asset
        // no se renombra (misma regla que HU-29).
        assertEquals("trapecios_con_apoyo_banco_inclinado_mancuernas", exercise.mediaResource)
    }

    @Test
    fun `the suggested equipment of the new exercise is its first listed option`() {
        // Regla de CA-41.07: la sugerencia del plan es el valor atómico de la primera
        // opción listada. Aquí se fija el lado del catálogo; DefaultPlanTest fija el otro.
        assertEquals(EquipmentCatalog.MANCUERNA, ExerciseCatalog.byId(38)!!.equipmentTypeIds.first())
    }

    @Test
    fun `exercises dropped from the default plan remain in the dictionary`() {
        listOf(27L, 33L, 11L).forEach { id ->
            assertNotNull("El ejercicio $id fue eliminado del catálogo", ExerciseCatalog.byId(id))
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
        "Extensión de Tríceps (Pushdown)",
        "Extensión de Tríceps sobre Cabeza",
        "Face Pull",
        "Rompecráneos",
        "Vuelos Posteriores (Pájaros)",
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

    /**
     * CA-41.03: la dificultad de progresión de los 37 existentes **no se modifica**. El
     * único movimiento es el `MEDIUM` que trae el ejercicio 38 (16 → 17).
     */
    @Test
    fun `difficulty distribution splits the catalog into 13 high, 8 low and 17 medium`() {
        val byDifficulty = ExerciseCatalog.ALL.groupingBy { it.progressionDifficulty }.eachCount()

        assertEquals(13, byDifficulty[ProgressionDifficulty.HIGH])
        assertEquals(8, byDifficulty[ProgressionDifficulty.LOW])
        assertEquals(17, byDifficulty[ProgressionDifficulty.MEDIUM])
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
