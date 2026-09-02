package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedExercise
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Catálogo base de ejercicios precargado en instalación fresca.
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

    // Tipos de equipamiento (equipment_type.id)
    private const val MAQUINA = 1L
    private const val MANCUERNAS = 2L
    private const val MANCUERNA = 5L
    private const val POLEA = 6L
    private const val MANCUERNAS_O_POLEA = 10L
    private const val POLEA_CON_CUERDA = 11L
    private const val POLEA_CUERDA_O_BARRA_V = 12L
    private const val BARRA = 13L
    private const val MANCUERNA_POLEA_O_BARRA = 14L
    private const val BARRA_O_MANCUERNAS = 15L
    private const val BARRA_FIJA = 23L

    val ALL: List<SeedExercise> = listOf(
        SeedExercise(1, "Aductores", MAQUINA, listOf(ADUCTORES), "aductores_maquina"),
        SeedExercise(
            2,
            "Cruce de Polea Alta",
            POLEA,
            listOf(PECHO_INFERIOR),
            "cruce_de_polea_alta_polea",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(3, "Crunch Abdominal", POLEA, listOf(ABDOMEN), "crunch_abdominal_polea"),
        SeedExercise(
            4,
            "Curl Bayesian en Banco Inclinado",
            MANCUERNAS,
            listOf(BICEPS),
            "curl_bayesian_en_banco_inclinado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            5,
            "Curl de Concentración",
            MANCUERNA,
            listOf(BICEPS),
            "curl_de_concentracion_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            6,
            "Curl de Isquiotibiales Sentado",
            MAQUINA,
            listOf(ISQUIOTIBIALES),
            "curl_de_isquiotibiales_sentado_maquina",
        ),
        SeedExercise(
            7,
            "Curl de Martillo Cruzado",
            MANCUERNAS,
            listOf(BICEPS),
            "curl_de_martillo_cruzado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            8,
            "Curl de Predicador",
            MANCUERNA,
            listOf(BICEPS),
            "curl_de_predicador_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            9,
            "Elevación de Pantorrilla en Máquina de Pie",
            MAQUINA,
            listOf(GEMELOS),
            "elevacion_de_pantorrilla_en_maquina_de_pie_maquina",
        ),
        SeedExercise(
            10,
            "Elevación Lateral",
            MANCUERNAS_O_POLEA,
            listOf(HOMBRO),
            "elevacion_lateral_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(11, "Extensión de Cuádriceps", MAQUINA, listOf(CUADRICEPS), "extension_de_cuadriceps_maquina"),
        SeedExercise(
            12,
            "Extensión de Tríceps en Polea (Pushdown)",
            POLEA_CUERDA_O_BARRA_V,
            listOf(TRICEPS),
            "extension_de_triceps_en_polea_pushdown_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            13,
            "Extensión de Tríceps por encima de la Cabeza",
            MANCUERNAS_O_POLEA,
            listOf(TRICEPS),
            "extension_de_triceps_por_encima_de_la_cabeza_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            14,
            "Face Pull",
            POLEA_CON_CUERDA,
            listOf(ESPALDA_ALTA),
            "face_pull_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            15,
            "Hip Thrust",
            MAQUINA,
            listOf(GLUTEOS),
            "hip_thrust_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            16,
            "Peso Muerto Rumano",
            BARRA,
            listOf(ISQUIOTIBIALES, GLUTEOS),
            "peso_muerto_rumano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            17,
            "Prensa Inclinada",
            MAQUINA,
            listOf(CUADRICEPS),
            "prensa_inclinada_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            18,
            "Press de Banca Inclinado",
            MANCUERNA_POLEA_O_BARRA,
            listOf(PECHO_SUPERIOR),
            "press_de_banca_inclinado_mancuerna",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            19,
            "Press de Banca Plano",
            BARRA_O_MANCUERNAS,
            listOf(PECHO_MEDIO),
            "press_de_banca_plano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(20, "Press Pallof", POLEA, listOf(ABDOMEN), "press_pallof_polea"),
        SeedExercise(
            21,
            "Remo T Inclinado",
            MAQUINA,
            listOf(ESPALDA_MEDIA),
            "remo_t_inclinado_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            22,
            "Sentadilla Búlgara",
            MANCUERNAS,
            listOf(CUADRICEPS, GLUTEOS),
            "sentadilla_bulgara_mancuernas",
        ),
        SeedExercise(
            23,
            "Sentadilla de Zumo",
            MANCUERNA,
            listOf(CUADRICEPS, ADUCTORES),
            "sentadilla_de_zumo_mancuerna",
        ),
        SeedExercise(
            24,
            "Sentadilla Hack",
            MAQUINA,
            listOf(CUADRICEPS),
            "sentadilla_hack_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Renombrado desde "Tirón de Dorsales" (HU-29): mismo movimiento, conserva id,
        // equipamiento, zona muscular, recurso visual e historial.
        SeedExercise(25, "Jalón al Pecho", POLEA, listOf(DORSAL_ANCHO), "tiron_de_dorsales_polea"),
        SeedExercise(
            26,
            "Vuelos Posteriores",
            MANCUERNAS,
            listOf(HOMBRO),
            "vuelos_posteriores_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Recatalogado en HU-29: movimiento de Espalda Alta, no de Hombro y Trapecio.
        SeedExercise(27, "Remo al Mentón", BARRA, listOf(ESPALDA_ALTA), "remo_al_menton_barra"),
        SeedExercise(
            28,
            "Aperturas",
            MAQUINA,
            listOf(PECHO_MEDIO),
            "aperturas_contractor",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(29, "Pull-Over", POLEA, listOf(DORSAL_ANCHO), "pull_over_polea"),
        SeedExercise(
            30,
            "Curl Martillo",
            MANCUERNAS,
            listOf(BICEPS),
            "curl_martillo_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            31,
            "Rompecráneos",
            BARRA,
            listOf(TRICEPS),
            "rompecraneos_barra",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(32, "Remo Horizontal", POLEA, listOf(ESPALDA_MEDIA), "remo_horizontal_polea"),
        SeedExercise(33, "Zancadas", MANCUERNAS, listOf(CUADRICEPS, GLUTEOS), "zancadas_mancuernas"),
        SeedExercise(
            34,
            "Press Militar",
            MANCUERNAS,
            listOf(HOMBRO),
            "press_militar_mancuernas",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(35, "Dominadas", BARRA_FIJA, listOf(DORSAL_ANCHO), "dominadas_barra_fija", isBodyweight = true),
        SeedExercise(
            36,
            "Remo Unilateral en Polea Baja",
            POLEA,
            listOf(ESPALDA_MEDIA),
            "remo_unilateral_en_polea_baja_polea",
        ),
        SeedExercise(
            37,
            "Remo Unilateral en Polea Alta",
            POLEA,
            listOf(ESPALDA_ALTA),
            "remo_unilateral_en_polea_alta_polea",
        ),
    )

    fun byId(id: Long): SeedExercise? = ALL.firstOrNull { it.id == id }
}
