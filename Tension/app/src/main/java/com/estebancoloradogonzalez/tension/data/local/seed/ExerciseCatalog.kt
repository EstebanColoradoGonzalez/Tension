package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedExercise
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Catálogo base de ejercicios precargado en instalación fresca: **38 ejercicios**.
 *
 * Cada ejercicio declara **la lista de implementos con los que se puede hacer** (HU-39),
 * siempre al menos uno. El equipamiento dejó de ser identidad del ejercicio: el mismo
 * movimiento con distintos implementos es un solo ejercicio con varias opciones, y la
 * serie registra cuál se usó. Por eso el nombre ya no lleva el implemento dentro y ocho
 * ejercicios se renombraron para quitárselo (CA-39.07).
 *
 * Desde HU-41 la clasificación muscular es **jerárquica**: zonas principales, las que
 * ejecutan el movimiento, y secundarias, las que asisten. El criterio sigue siendo
 * biomecánico —el músculo que trabaja, no la máquina ni la ubicación aparente— pero la
 * granularidad es anatómica: los tres deltoides y el manguito rotador en lugar de un
 * genérico «Hombro», las cabezas del bíceps y del tríceps, braquial y braquiorradial, las
 * porciones del pectoral. Ver [MuscleZoneCatalog].
 *
 * La dificultad de progresión clasifica la capacidad intrínseca de avance del
 * ejercicio: `HIGH` para el aislamiento de zonas pequeñas, donde el salto mínimo
 * disponible pesa mucho sobre la carga habitual; `LOW` para los compuestos
 * multiarticulares pesados; `MEDIUM` (valor por defecto, no anotado) para el resto.
 *
 * Ningún ejercicio se elimina jamás del catálogo. Los que no forman parte del plan
 * predeterminado siguen disponibles como alternativa de slot o para asignación manual, y
 * hoy son ocho. Un ejercicio renombrado conserva su identificador, su recurso visual, su
 * clasificación muscular y su historial — el asset **no** se renombra.
 */
object ExerciseCatalog {

    // Zonas musculares (muscle_zone.id) — ver MuscleZoneCatalog
    private const val PECTORAL_SUPERIOR = MuscleZoneCatalog.PECTORAL_SUPERIOR
    private const val PECTORAL_MEDIO = MuscleZoneCatalog.PECTORAL_MEDIO
    private const val PECTORAL_INFERIOR = MuscleZoneCatalog.PECTORAL_INFERIOR
    private const val PECTORAL_MAYOR = MuscleZoneCatalog.PECTORAL_MAYOR
    private const val DELTOIDES_ANTERIOR = MuscleZoneCatalog.DELTOIDES_ANTERIOR
    private const val DELTOIDES_LATERAL = MuscleZoneCatalog.DELTOIDES_LATERAL
    private const val DELTOIDES_POSTERIOR = MuscleZoneCatalog.DELTOIDES_POSTERIOR
    private const val MANGUITO_ROTADOR = MuscleZoneCatalog.MANGUITO_ROTADOR
    private const val DORSAL_ANCHO = MuscleZoneCatalog.DORSAL_ANCHO
    private const val ESPALDA_ALTA = MuscleZoneCatalog.ESPALDA_ALTA
    private const val TRAPECIO = MuscleZoneCatalog.TRAPECIO
    private const val TRAPECIO_SUPERIOR = MuscleZoneCatalog.TRAPECIO_SUPERIOR
    private const val TRAPECIO_INFERIOR = MuscleZoneCatalog.TRAPECIO_INFERIOR
    private const val ROMBOIDES = MuscleZoneCatalog.ROMBOIDES
    private const val ERECTORES_ESPINALES = MuscleZoneCatalog.ERECTORES_ESPINALES
    private const val BICEPS_BRAQUIAL = MuscleZoneCatalog.BICEPS_BRAQUIAL
    private const val BICEPS_CABEZA_LARGA = MuscleZoneCatalog.BICEPS_CABEZA_LARGA
    private const val BICEPS_CABEZA_CORTA = MuscleZoneCatalog.BICEPS_CABEZA_CORTA
    private const val TRICEPS_BRAQUIAL = MuscleZoneCatalog.TRICEPS_BRAQUIAL
    private const val TRICEPS_CABEZA_LARGA = MuscleZoneCatalog.TRICEPS_CABEZA_LARGA
    private const val TRICEPS_CABEZA_LATERAL = MuscleZoneCatalog.TRICEPS_CABEZA_LATERAL
    private const val TRICEPS_CABEZA_MEDIAL = MuscleZoneCatalog.TRICEPS_CABEZA_MEDIAL
    private const val BRAQUIAL = MuscleZoneCatalog.BRAQUIAL
    private const val BRAQUIORRADIAL = MuscleZoneCatalog.BRAQUIORRADIAL
    private const val RECTO_ABDOMINAL = MuscleZoneCatalog.RECTO_ABDOMINAL
    private const val OBLICUOS = MuscleZoneCatalog.OBLICUOS
    private const val CUADRICEPS = MuscleZoneCatalog.CUADRICEPS
    private const val ISQUIOTIBIALES = MuscleZoneCatalog.ISQUIOTIBIALES
    private const val GLUTEO_MAYOR = MuscleZoneCatalog.GLUTEO_MAYOR
    private const val ADUCTORES = MuscleZoneCatalog.ADUCTORES
    private const val GASTROCNEMIO = MuscleZoneCatalog.GASTROCNEMIO

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
            primaryMuscleZoneIds = listOf(ADUCTORES),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "aductores_maquina",
        ),
        SeedExercise(
            2,
            "Cruce de Polea Alta",
            listOf(POLEA),
            primaryMuscleZoneIds = listOf(PECTORAL_INFERIOR),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR),
            mediaResource = "cruce_de_polea_alta_polea",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            3,
            "Crunch Abdominal",
            listOf(PESO_CORPORAL, POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(RECTO_ABDOMINAL),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "crunch_abdominal_polea",
        ),
        SeedExercise(
            4,
            "Curl Bayesian en Banco Inclinado",
            listOf(POLEA, MANCUERNA),
            primaryMuscleZoneIds = listOf(BICEPS_CABEZA_LARGA),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "curl_bayesian_en_banco_inclinado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            5,
            "Curl de Concentración",
            listOf(MANCUERNA, POLEA),
            primaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "curl_de_concentracion_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            6,
            "Curl de Isquiotibiales Sentado",
            listOf(MAQUINA),
            primaryMuscleZoneIds = listOf(ISQUIOTIBIALES),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "curl_de_isquiotibiales_sentado_maquina",
        ),
        SeedExercise(
            7,
            "Curl de Martillo Cruzado",
            listOf(MANCUERNA, POLEA),
            primaryMuscleZoneIds = listOf(BRAQUIAL, BRAQUIORRADIAL),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL),
            mediaResource = "curl_de_martillo_cruzado_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            8,
            "Curl de Predicador",
            listOf(BARRA, MANCUERNA, MAQUINA, POLEA),
            primaryMuscleZoneIds = listOf(BICEPS_CABEZA_CORTA),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "curl_de_predicador_mancuerna",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Renombrado en HU-39 desde "Elevación de Pantorrilla en Máquina de Pie": el nombre
        // llevaba el implemento dentro y el ejercicio se hace en Máquina o en Máquina Smith.
        SeedExercise(
            9,
            "Elevación de Pantorrilla de Pie",
            listOf(MAQUINA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(GASTROCNEMIO),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "elevacion_de_pantorrilla_en_maquina_de_pie_maquina",
        ),
        SeedExercise(
            10,
            "Elevación Lateral",
            listOf(MANCUERNA, POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(DELTOIDES_LATERAL),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "elevacion_lateral_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            11,
            "Extensión de Cuádriceps",
            listOf(MAQUINA),
            primaryMuscleZoneIds = listOf(CUADRICEPS),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "extension_de_cuadriceps_maquina",
        ),
        // Renombrado en HU-39 desde "Extensión de Tríceps en Polea (Pushdown)": ya no
        // necesita decir «en Polea».
        SeedExercise(
            12,
            "Extensión de Tríceps (Pushdown)",
            listOf(POLEA),
            primaryMuscleZoneIds = listOf(TRICEPS_CABEZA_LATERAL, TRICEPS_CABEZA_MEDIAL),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "extension_de_triceps_en_polea_pushdown_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Renombrado en HU-39 desde "Extensión de Tríceps por encima de la Cabeza".
        SeedExercise(
            13,
            "Extensión de Tríceps sobre Cabeza",
            listOf(MANCUERNA, BARRA, POLEA),
            primaryMuscleZoneIds = listOf(TRICEPS_CABEZA_LARGA),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "extension_de_triceps_por_encima_de_la_cabeza_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            14,
            "Face Pull",
            listOf(POLEA, BANDA_ELASTICA),
            primaryMuscleZoneIds = listOf(DELTOIDES_POSTERIOR, TRAPECIO),
            secondaryMuscleZoneIds = listOf(MANGUITO_ROTADOR),
            mediaResource = "face_pull_polea_con_cuerda",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            15,
            "Hip Thrust",
            listOf(BARRA, MAQUINA, MANCUERNA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(GLUTEO_MAYOR),
            secondaryMuscleZoneIds = listOf(ISQUIOTIBIALES, CUADRICEPS),
            mediaResource = "hip_thrust_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            16,
            "Peso Muerto Rumano",
            listOf(BARRA, MANCUERNA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(ISQUIOTIBIALES, GLUTEO_MAYOR),
            secondaryMuscleZoneIds = listOf(ERECTORES_ESPINALES),
            mediaResource = "peso_muerto_rumano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            17,
            "Prensa Inclinada",
            listOf(MAQUINA),
            primaryMuscleZoneIds = listOf(CUADRICEPS, GLUTEO_MAYOR),
            secondaryMuscleZoneIds = listOf(ISQUIOTIBIALES),
            mediaResource = "prensa_inclinada_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            18,
            "Press de Banca Inclinado",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR, TRICEPS_BRAQUIAL),
            mediaResource = "press_de_banca_inclinado_mancuerna",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            19,
            "Press de Banca Plano",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(PECTORAL_MEDIO),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR, TRICEPS_BRAQUIAL),
            mediaResource = "press_de_banca_plano_barra",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Recatalogado en HU-41: `Core` no es una zona sino una región, y el movimiento
        // anti-rotación lo ejecutan los oblicuos con el recto abdominal asistiendo.
        SeedExercise(
            20,
            "Press Pallof",
            listOf(POLEA, BANDA_ELASTICA),
            primaryMuscleZoneIds = listOf(OBLICUOS),
            secondaryMuscleZoneIds = listOf(RECTO_ABDOMINAL),
            mediaResource = "press_pallof_polea",
        ),
        SeedExercise(
            21,
            "Remo T Inclinado",
            listOf(BARRA, MAQUINA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO, TRAPECIO),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL, ROMBOIDES),
            mediaResource = "remo_t_inclinado_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        SeedExercise(
            22,
            "Sentadilla Búlgara",
            listOf(PESO_CORPORAL, MANCUERNA, BARRA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(CUADRICEPS, GLUTEO_MAYOR),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "sentadilla_bulgara_mancuernas",
        ),
        // Renombrado en HU-39 desde "Sentadilla de Zumo": el nombre correcto del patrón
        // es sumo.
        SeedExercise(
            23,
            "Sentadilla Sumo",
            listOf(MANCUERNA, PESA_RUSA, BARRA, POLEA),
            primaryMuscleZoneIds = listOf(GLUTEO_MAYOR, CUADRICEPS),
            secondaryMuscleZoneIds = listOf(ADUCTORES),
            mediaResource = "sentadilla_de_zumo_mancuerna",
        ),
        SeedExercise(
            24,
            "Sentadilla Hack",
            listOf(MAQUINA, BARRA),
            primaryMuscleZoneIds = listOf(CUADRICEPS),
            secondaryMuscleZoneIds = listOf(GLUTEO_MAYOR),
            mediaResource = "sentadilla_hack_maquina",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Renombrado desde "Tirón de Dorsales" (HU-29): mismo movimiento, conserva id,
        // zona muscular, recurso visual e historial.
        SeedExercise(
            25,
            "Jalón al Pecho",
            listOf(POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL, ESPALDA_ALTA),
            mediaResource = "tiron_de_dorsales_polea",
        ),
        // Renombrado en HU-39 desde "Vuelos Posteriores".
        SeedExercise(
            26,
            "Vuelos Posteriores (Pájaros)",
            listOf(MANCUERNA, POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(DELTOIDES_POSTERIOR),
            secondaryMuscleZoneIds = listOf(TRAPECIO, ROMBOIDES),
            mediaResource = "vuelos_posteriores_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        // Recatalogado dos veces. HU-29 lo movió de «Hombro y Trapecio» a Espalda Alta
        // cuando el catálogo no tenía con qué distinguirlos; HU-41, con los tres deltoides
        // y las porciones del trapecio disponibles, lo devuelve a lo que el movimiento
        // hace: deltoides lateral y trapecio superior ejecutan, el bíceps asiste.
        SeedExercise(
            27,
            "Remo al Mentón",
            listOf(BARRA, POLEA, MANCUERNA),
            primaryMuscleZoneIds = listOf(DELTOIDES_LATERAL, TRAPECIO_SUPERIOR),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL),
            mediaResource = "remo_al_menton_barra",
        ),
        SeedExercise(
            28,
            "Aperturas",
            listOf(MANCUERNA, POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(PECTORAL_MAYOR),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR),
            mediaResource = "aperturas_contractor",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            29,
            "Pull-Over",
            listOf(MANCUERNA, POLEA, BARRA, MAQUINA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO),
            secondaryMuscleZoneIds = listOf(PECTORAL_INFERIOR),
            mediaResource = "pull_over_polea",
        ),
        SeedExercise(
            30,
            "Curl Martillo",
            listOf(MANCUERNA, POLEA),
            primaryMuscleZoneIds = listOf(BRAQUIAL, BRAQUIORRADIAL),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL),
            mediaResource = "curl_martillo_mancuernas",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            31,
            "Rompecráneos",
            listOf(BARRA, MANCUERNA, POLEA),
            primaryMuscleZoneIds = listOf(TRICEPS_BRAQUIAL),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "rompecraneos_barra",
            progressionDifficulty = ProgressionDifficulty.HIGH,
        ),
        SeedExercise(
            32,
            "Remo Horizontal",
            listOf(BARRA, MANCUERNA, POLEA, MAQUINA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO),
            secondaryMuscleZoneIds = listOf(TRAPECIO, ROMBOIDES, BICEPS_BRAQUIAL),
            mediaResource = "remo_horizontal_polea",
        ),
        // Renombrado en HU-39 desde "Zancadas".
        SeedExercise(
            33,
            "Zancadas (Lunges)",
            listOf(PESO_CORPORAL, MANCUERNA, BARRA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(CUADRICEPS, GLUTEO_MAYOR),
            secondaryMuscleZoneIds = emptyList(),
            mediaResource = "zancadas_mancuernas",
        ),
        SeedExercise(
            34,
            "Press Militar",
            listOf(BARRA, MANCUERNA, MAQUINA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR, DELTOIDES_LATERAL),
            secondaryMuscleZoneIds = listOf(TRICEPS_BRAQUIAL),
            mediaResource = "press_militar_mancuernas",
            progressionDifficulty = ProgressionDifficulty.LOW,
        ),
        // Único ejercicio semilla de peso corporal. De sus tres opciones solo
        // `Peso Añadido` habilita la captura de carga: `Barra Fija` es la dominada
        // estricta y `Máquina` la asistida, cuyo contrapeso resta esfuerzo (CA-39.05).
        SeedExercise(
            35,
            "Dominadas",
            listOf(BARRA_FIJA, MAQUINA, PESO_ANADIDO),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL, TRAPECIO_INFERIOR),
            mediaResource = "dominadas_barra_fija",
            isBodyweight = true,
        ),
        // Renombrado en HU-39 desde "Remo Unilateral en Polea Baja".
        SeedExercise(
            36,
            "Remo Unilateral Polea Baja",
            listOf(POLEA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL, ROMBOIDES),
            mediaResource = "remo_unilateral_en_polea_baja_polea",
        ),
        // Renombrado en HU-39 desde "Remo Unilateral en Polea Alta". Sale del plan por
        // defecto en HU-41 y permanece en el Diccionario.
        SeedExercise(
            37,
            "Remo Unilateral Polea Alta",
            listOf(POLEA),
            primaryMuscleZoneIds = listOf(DORSAL_ANCHO, ESPALDA_ALTA),
            secondaryMuscleZoneIds = listOf(BICEPS_BRAQUIAL),
            mediaResource = "remo_unilateral_en_polea_alta_polea",
        ),
        // Añadido en HU-41: ocupa el cuarto puesto del viernes que deja Remo Unilateral
        // Polea Alta. El `mediaResource` no lleva el «en» que el nombre sí tiene, porque
        // el asset manda sobre el nombre y no se renombra (misma regla que HU-29).
        SeedExercise(
            38,
            "Trapecios con Apoyo en Banco Inclinado",
            listOf(MANCUERNA, BARRA, MAQUINA_SMITH),
            primaryMuscleZoneIds = listOf(TRAPECIO, TRAPECIO_INFERIOR),
            secondaryMuscleZoneIds = listOf(ROMBOIDES, DELTOIDES_POSTERIOR),
            mediaResource = "trapecios_con_apoyo_banco_inclinado_mancuernas",
        ),
    )

    fun byId(id: Long): SeedExercise? = ALL.firstOrNull { it.id == id }
}
