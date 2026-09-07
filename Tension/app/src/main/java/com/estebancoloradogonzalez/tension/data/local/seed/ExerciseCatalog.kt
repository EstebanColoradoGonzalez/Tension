package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedExercise
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Catálogo base de ejercicios precargado en instalación fresca.
 *
 * Cada ejercicio declara **la lista de implementos con los que se puede hacer** (HU-39),
 * siempre al menos uno. El equipamiento dejó de ser identidad del ejercicio: el mismo
 * movimiento con distintos implementos es un solo ejercicio con varias opciones, y la
 * serie registra cuál se usó. Por eso el nombre ya no lleva el implemento dentro y ocho
 * ejercicios se renombraron para quitárselo (CA-39.07).
 *
 * La zona muscular se asigna por criterio biomecánico: el músculo que ejecuta el
 * movimiento, no la máquina ni la ubicación aparente.
 *
 * La dificultad de progresión clasifica la capacidad intrínseca de avance del
 * ejercicio: `HIGH` para el aislamiento de zonas pequeñas, donde el salto mínimo
 * disponible pesa mucho sobre la carga habitual; `LOW` para los compuestos
 * multiarticulares pesados; `MEDIUM` (valor por defecto, no anotado) para el resto.
 *
 * Ningún ejercicio se elimina jamás del catálogo. Los que no forman parte del plan
 * predeterminado siguen disponibles como alternativa de slot o para asignación manual.
 * Un ejercicio renombrado conserva su identificador, su recurso visual, su clasificación
 * muscular y su historial — el asset **no** se renombra.
 */
object ExerciseCatalog {

    // Zonas musculares (muscle_zone.id)
    private const val PECHO_MEDIO = 1L
    private const val PECHO_SUPERIOR = 2L
    private const val PECHO_INFERIOR = 3L
    private const val ESPALDA_MEDIA = 4L
    private const val DORSAL_ANCHO = 5L
    private const val ABDOMEN = 6L
    private const val HOMBRO = 7L
    private const val TRICEPS = 8L
    private const val BICEPS = 9L
    private const val CUADRICEPS = 10L
    private const val ISQUIOTIBIALES = 11L
    private const val ADUCTORES = 12L
    private const val GEMELOS = 14L
    private const val GLUTEOS = 15L
    private const val ESPALDA_ALTA = 16L

    // Tipos de equipamiento (equipment_type.id) — ver EquipmentCatalog
    private const val MAQUINA = EquipmentCatalog.MAQUINA
    private const val MAQUINA_SMITH = EquipmentCatalog.MAQUINA_SMITH
    private const val POLEA = EquipmentCatalog.POLEA
    private const val BARRA = EquipmentCatalog.BARRA
    private const val BARRA_FIJA = EquipmentCatalog.BARRA_FIJA
    private const val MANCUERNA = EquipmentCatalog.MANCUERNA
    private const val PESA_RUSA = EquipmentCatalog.PESA_RUSA
    private const val BANDA_ELASTICA = EquipmentCatalog.BANDA_ELASTICA
    private const val PESO_CORPORAL = EquipmentCatalog.PESO_CORPORAL
    private const val PESO_ANADIDO = EquipmentCatalog.PESO_ANADIDO

    val ALL: List<SeedExercise> = listOf(
        SeedExercise(
            1,
            "Aductores",
            listOf(MAQUINA, POLEA, BANDA_ELASTICA),
            listOf(ADUCTORES),
            "aductores_maquina",
        ),
        SeedExercise(
            2,
            "Cruce de Polea Alta",
            listOf(POLEA),
            listOf(PECHO_INFERIOR),
            "cruce_de_polea_alta_polea",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            3,
            "Crunch Abdominal",
            listOf(PESO_CORPORAL, POLEA, MAQUINA),
            listOf(ABDOMEN),
            "crunch_abdominal_polea",
        ),
        SeedExercise(
            4,
            "Curl Bayesian en Banco Inclinado",
            listOf(POLEA, MANCUERNA),
            listOf(BICEPS),
            "curl_bayesian_en_banco_inclinado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            5,
            "Curl de Concentración",
            listOf(MANCUERNA, POLEA),
            listOf(BICEPS),
            "curl_de_concentracion_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            6,
            "Curl de Isquiotibiales Sentado",
            listOf(MAQUINA),
            listOf(ISQUIOTIBIALES),
            "curl_de_isquiotibiales_sentado_maquina",
        ),
        SeedExercise(
            7,
            "Curl de Martillo Cruzado",
            listOf(MANCUERNA, POLEA),
            listOf(BICEPS),
            "curl_de_martillo_cruzado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            8,
            "Curl de Predicador",
            listOf(BARRA, MANCUERNA, MAQUINA, POLEA),
            listOf(BICEPS),
            "curl_de_predicador_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Renombrado en HU-39 desde "Elevación de Pantorrilla en Máquina de Pie": el nombre
        // llevaba el implemento dentro y el ejercicio se hace en Máquina o en Máquina Smith.
        SeedExercise(
            9,
            "Elevación de Pantorrilla de Pie",
            listOf(MAQUINA, MAQUINA_SMITH),
            listOf(GEMELOS),
            "elevacion_de_pantorrilla_en_maquina_de_pie_maquina",
        ),
        SeedExercise(
            10,
            "Elevación Lateral",
            listOf(MANCUERNA, POLEA, MAQUINA),
            listOf(HOMBRO),
            "elevacion_lateral_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            11,
            "Extensión de Cuádriceps",
            listOf(MAQUINA),
            listOf(CUADRICEPS),
            "extension_de_cuadriceps_maquina",
        ),
        // Renombrado en HU-39 desde "Extensión de Tríceps en Polea (Pushdown)": ya no
        // necesita decir «en Polea».
        SeedExercise(
            12,
            "Extensión de Tríceps (Pushdown)",
            listOf(POLEA),
            listOf(TRICEPS),
            "extension_de_triceps_en_polea_pushdown_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Renombrado en HU-39 desde "Extensión de Tríceps por encima de la Cabeza".
        SeedExercise(
            13,
            "Extensión de Tríceps sobre Cabeza",
            listOf(MANCUERNA, BARRA, POLEA),
            listOf(TRICEPS),
            "extension_de_triceps_por_encima_de_la_cabeza_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            14,
            "Face Pull",
            listOf(POLEA, BANDA_ELASTICA),
            listOf(ESPALDA_ALTA),
            "face_pull_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            15,
            "Hip Thrust",
            listOf(BARRA, MAQUINA, MANCUERNA, MAQUINA_SMITH),
            listOf(GLUTEOS),
            "hip_thrust_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            16,
            "Peso Muerto Rumano",
            listOf(BARRA, MANCUERNA, MAQUINA_SMITH),
            listOf(ISQUIOTIBIALES, GLUTEOS),
            "peso_muerto_rumano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            17,
            "Prensa Inclinada",
            listOf(MAQUINA),
            listOf(CUADRICEPS),
            "prensa_inclinada_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            18,
            "Press de Banca Inclinado",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            listOf(PECHO_SUPERIOR),
            "press_de_banca_inclinado_mancuerna",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            19,
            "Press de Banca Plano",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            listOf(PECHO_MEDIO),
            "press_de_banca_plano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            20,
            "Press Pallof",
            listOf(POLEA, BANDA_ELASTICA),
            listOf(ABDOMEN),
            "press_pallof_polea",
        ),
        SeedExercise(
            21,
            "Remo T Inclinado",
            listOf(BARRA, MAQUINA),
            listOf(ESPALDA_MEDIA),
            "remo_t_inclinado_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            22,
            "Sentadilla Búlgara",
            listOf(PESO_CORPORAL, MANCUERNA, BARRA, MAQUINA_SMITH),
            listOf(CUADRICEPS, GLUTEOS),
            "sentadilla_bulgara_mancuernas",
        ),
        // Renombrado en HU-39 desde "Sentadilla de Zumo": el nombre correcto del patrón
        // es sumo.
        SeedExercise(
            23,
            "Sentadilla Sumo",
            listOf(MANCUERNA, PESA_RUSA, BARRA, POLEA),
            listOf(CUADRICEPS, ADUCTORES),
            "sentadilla_de_zumo_mancuerna",
        ),
        SeedExercise(
            24,
            "Sentadilla Hack",
            listOf(MAQUINA, BARRA),
            listOf(CUADRICEPS),
            "sentadilla_hack_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Renombrado desde "Tirón de Dorsales" (HU-29): mismo movimiento, conserva id,
        // zona muscular, recurso visual e historial.
        SeedExercise(
            25,
            "Jalón al Pecho",
            listOf(POLEA, MAQUINA),
            listOf(DORSAL_ANCHO),
            "tiron_de_dorsales_polea",
        ),
        // Renombrado en HU-39 desde "Vuelos Posteriores".
        SeedExercise(
            26,
            "Vuelos Posteriores (Pájaros)",
            listOf(MANCUERNA, POLEA, MAQUINA),
            listOf(HOMBRO),
            "vuelos_posteriores_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Recatalogado en HU-29: movimiento de Espalda Alta, no de Hombro y Trapecio.
        SeedExercise(
            27,
            "Remo al Mentón",
            listOf(BARRA, POLEA, MANCUERNA),
            listOf(ESPALDA_ALTA),
            "remo_al_menton_barra",
        ),
        SeedExercise(
            28,
            "Aperturas",
            listOf(MANCUERNA, POLEA, MAQUINA),
            listOf(PECHO_MEDIO),
            "aperturas_contractor",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            29,
            "Pull-Over",
            listOf(MANCUERNA, POLEA, BARRA, MAQUINA),
            listOf(DORSAL_ANCHO),
            "pull_over_polea",
        ),
        SeedExercise(
            30,
            "Curl Martillo",
            listOf(MANCUERNA, POLEA),
            listOf(BICEPS),
            "curl_martillo_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            31,
            "Rompecráneos",
            listOf(BARRA, MANCUERNA, POLEA),
            listOf(TRICEPS),
            "rompecraneos_barra",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            32,
            "Remo Horizontal",
            listOf(BARRA, MANCUERNA, POLEA, MAQUINA),
            listOf(ESPALDA_MEDIA),
            "remo_horizontal_polea",
        ),
        // Renombrado en HU-39 desde "Zancadas".
        SeedExercise(
            33,
            "Zancadas (Lunges)",
            listOf(PESO_CORPORAL, MANCUERNA, BARRA, MAQUINA_SMITH),
            listOf(CUADRICEPS, GLUTEOS),
            "zancadas_mancuernas",
        ),
        SeedExercise(
            34,
            "Press Militar",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            listOf(HOMBRO),
            "press_militar_mancuernas",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Único ejercicio semilla de peso corporal. De sus tres opciones solo
        // `Peso Añadido` habilita la captura de carga: `Barra Fija` es la dominada
        // estricta y `Máquina` la asistida, cuyo contrapeso resta esfuerzo (CA-39.05).
        SeedExercise(
            35,
            "Dominadas",
            listOf(BARRA_FIJA, MAQUINA, PESO_ANADIDO),
            listOf(DORSAL_ANCHO),
            "dominadas_barra_fija",
            isBodyweight = true,
        ),
        // Renombrado en HU-39 desde "Remo Unilateral en Polea Baja".
        SeedExercise(
            36,
            "Remo Unilateral Polea Baja",
            listOf(POLEA),
            listOf(ESPALDA_MEDIA),
            "remo_unilateral_en_polea_baja_polea",
        ),
        // Renombrado en HU-39 desde "Remo Unilateral en Polea Alta".
        SeedExercise(
            37,
            "Remo Unilateral Polea Alta",
            listOf(POLEA),
            listOf(ESPALDA_ALTA),
            "remo_unilateral_en_polea_alta_polea",
        ),
    )

    fun byId(id: Long): SeedExercise? = ALL.firstOrNull { it.id == id }
}
