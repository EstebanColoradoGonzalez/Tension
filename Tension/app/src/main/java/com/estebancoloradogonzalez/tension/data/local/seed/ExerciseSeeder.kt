package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object ExerciseSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        seedExercises(db)
        seedExerciseMuscleZones(db)
    }

    @Suppress("LongMethod")
    private fun seedExercises(db: SupportSQLiteDatabase) {
        // Module A (15 exercises, IDs 1-15)
        ex(db, 1, "Press de banca", "A", 1, media = "press_de_banca_maquina")
        ex(db, 2, "Press de mancuerna", "A", 2, media = "press_de_mancuerna_mancuernas")
        ex(db, 3, "Press de banca inclinada", "A", 1, media = "press_de_banca_inclinada_maquina")
        ex(db, 4, "Flexiones", "A", 4, bw = 1, fail = 1, media = "flexiones_cuerpo")
        ex(db, 5, "Cruce en polea alta", "A", 1, media = "cruce_en_polea_alta_maquina")
        ex(db, 6, "Apertura de pecho sentado", "A", 1, media = "apertura_de_pecho_sentado_maquina")
        ex(db, 7, "Apertura de pecho inclinado", "A", 1, media = "apertura_de_pecho_inclinado_maquina")
        ex(db, 8, "Remo con Inclinación", "A", 3, media = "remo_con_inclinacion_barra_de_pesas")
        ex(db, 9, "Remo con un solo brazo doblado", "A", 5, media = "remo_con_un_solo_brazo_doblado_mancuerna")
        ex(db, 10, "Tiro de dorsales (Agarre ancho)", "A", 1, media = "tiro_de_dorsales_agarre_ancho_maquina")
        ex(db, 11, "Abdominales", "A", 4, bw = 1, media = "abdominales_cuerpo")
        ex(db, 12, "Escalador", "A", 4, bw = 1, media = "escalador_cuerpo")
        ex(db, 13, "Giro Ruso", "A", 4, bw = 1, media = "giro_ruso_cuerpo")
        ex(db, 14, "Plancha", "A", 4, bw = 1, iso = 1, media = "plancha_cuerpo")
        ex(db, 15, "Plancha Lateral", "A", 4, bw = 1, iso = 1, media = "plancha_lateral_cuerpo")

        // Module B (14 exercises, IDs 16-29)
        ex(db, 16, "Curl de bíceps", "B", 5, media = "curl_de_biceps_mancuerna")
        ex(db, 17, "Curl de bíceps", "B", 6, media = "curl_de_biceps_polea")
        ex(db, 18, "Curl de martillo cruzado", "B", 5, media = "curl_de_martillo_cruzado_mancuerna")
        ex(db, 19, "Curl de martillo", "B", 5, media = "curl_de_martillo_mancuerna")
        ex(db, 20, "Curl de Contracción", "B", 5, media = "curl_de_contraccion_mancuerna")
        ex(db, 21, "Dominada de tríceps banco", "B", 7, media = "dominada_de_triceps_banco_pesa")
        ex(db, 22, "Extensión de tríceps por encima de la cabeza", "B", 5, media = "extension_de_triceps_por_encima_de_la_cabeza_mancuerna")
        ex(db, 23, "Flexión de tríceps con cuerda", "B", 1, media = "flexion_de_triceps_con_cuerda_maquina")
        ex(db, 24, "Elevación frontal", "B", 5, media = "elevacion_frontal_mancuerna")
        ex(db, 25, "Elevación lateral", "B", 5, media = "elevacion_lateral_mancuerna")
        ex(db, 26, "Elevación de hombros con mancuernas", "B", 5, media = "elevacion_de_hombros_con_mancuernas_mancuerna")
        ex(db, 27, "Press de elevación sentado", "B", 5, media = "press_de_elevacion_sentado_mancuerna")
        ex(db, 28, "Remo vertical", "B", 3, media = "remo_vertical_barra_de_pesas")
        ex(db, 29, "Remo vertical con cable", "B", 1, media = "remo_vertical_con_cable_maquina")

        // Module C (14 exercises, IDs 30-43)
        ex(db, 30, "Extensión de Cuádriceps", "C", 1, media = "extension_de_cuadriceps_maquina")
        ex(db, 31, "Curl Femoral Tumbado", "C", 1, media = "curl_femoral_tumbado_maquina")
        ex(db, 32, "Aductor de Cadera", "C", 1, media = "aductor_de_cadera_maquina")
        ex(db, 33, "Abductor de Cadera", "C", 1, media = "abductor_de_cadera_maquina")
        ex(db, 34, "Elevación de Gemelos Sentado", "C", 1, media = "elevacion_de_gemelos_sentado_maquina")
        ex(db, 35, "Empuje de Cadera", "C", 1, media = "empuje_de_cadera_maquina")
        ex(db, 36, "Sentadilla de Sumo", "C", 8, media = "sentadilla_de_sumo_mancuerna_o_pesa_rusa")
        ex(db, 37, "Sentadilla", "C", 4, bw = 1, media = "sentadilla_cuerpo")
        ex(db, 38, "Sentadilla Búlgara Dividida", "C", 2, media = "sentadilla_bulgara_dividida_mancuernas")
        ex(db, 39, "Sentadilla", "C", 9, media = "sentadilla_maquina_multiestacion")
        ex(db, 40, "Subir Escalones", "C", 1, media = "subir_escalones_maquina")
        ex(db, 41, "Zancada hacia atrás", "C", 2, media = "zancada_hacia_atras_mancuernas")
        ex(db, 42, "Avanzada de Zancadas", "C", 2, media = "avanzada_de_zancadas_mancuernas")
        ex(db, 43, "Press de Pierna", "C", 1, media = "press_de_pierna_maquina")
    }

    private fun ex(
        db: SupportSQLiteDatabase,
        id: Long,
        name: String,
        moduleCode: String,
        equipmentTypeId: Long,
        bw: Int = 0,
        iso: Int = 0,
        fail: Int = 0,
        media: String? = null,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("module_code", moduleCode)
            put("equipment_type_id", equipmentTypeId)
            put("is_bodyweight", bw)
            put("is_isometric", iso)
            put("is_to_technical_failure", fail)
            put("is_custom", 0)
            put("media_resource", media)
        }
        db.insert("exercise", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    @Suppress("LongMethod")
    private fun seedExerciseMuscleZones(db: SupportSQLiteDatabase) {
        // Module A — each exercise has 1 zone
        emz(db, 1, 1)   // Press de banca → Pecho Medio
        emz(db, 2, 1)   // Press de mancuerna → Pecho Medio
        emz(db, 3, 2)   // Press de banca inclinada → Pecho Superior
        emz(db, 4, 3)   // Flexiones → Pecho Inferior
        emz(db, 5, 3)   // Cruce en polea alta → Pecho Inferior
        emz(db, 6, 1)   // Apertura de pecho sentado → Pecho Medio
        emz(db, 7, 2)   // Apertura de pecho inclinado → Pecho Superior
        emz(db, 8, 4)   // Remo con Inclinación → Espalda Media
        emz(db, 9, 5)   // Remo con un solo brazo doblado → Dorsal Ancho
        emz(db, 10, 5)  // Tiro de dorsales → Dorsal Ancho
        emz(db, 11, 6)  // Abdominales → Abdomen
        emz(db, 12, 6)  // Escalador → Abdomen
        emz(db, 13, 6)  // Giro Ruso → Abdomen
        emz(db, 14, 6)  // Plancha → Abdomen
        emz(db, 15, 6)  // Plancha Lateral → Abdomen

        // Module B — each exercise has 1 zone
        emz(db, 16, 9)  // Curl de bíceps (Mancuerna) → Bíceps
        emz(db, 17, 9)  // Curl de bíceps (Polea) → Bíceps
        emz(db, 18, 9)  // Curl de martillo cruzado → Bíceps
        emz(db, 19, 9)  // Curl de martillo → Bíceps
        emz(db, 20, 9)  // Curl de Contracción → Bíceps
        emz(db, 21, 8)  // Dominada de tríceps banco → Tríceps
        emz(db, 22, 8)  // Extensión de tríceps → Tríceps
        emz(db, 23, 8)  // Flexión de tríceps con cuerda → Tríceps
        emz(db, 24, 7)  // Elevación frontal → Hombro
        emz(db, 25, 7)  // Elevación lateral → Hombro
        emz(db, 26, 7)  // Elevación de hombros con mancuernas → Hombro
        emz(db, 27, 7)  // Press de elevación sentado → Hombro
        emz(db, 28, 7)  // Remo vertical → Hombro
        emz(db, 29, 7)  // Remo vertical con cable → Hombro

        // Module C — single-zone exercises
        emz(db, 30, 10) // Extensión de Cuádriceps → Cuádriceps
        emz(db, 31, 11) // Curl Femoral Tumbado → Isquiotibiales
        emz(db, 32, 12) // Aductor de Cadera → Aductores
        emz(db, 33, 13) // Abductor de Cadera → Abductores
        emz(db, 34, 14) // Elevación de Gemelos Sentado → Gemelos
        emz(db, 35, 15) // Empuje de Cadera → Glúteos
        emz(db, 37, 10) // Sentadilla (Cuerpo) → Cuádriceps
        emz(db, 39, 10) // Sentadilla (Máquina Multiestación) → Cuádriceps
        emz(db, 43, 10) // Press de Pierna → Cuádriceps

        // Module C — multi-zone exercises (5 exercises × 2 zones = 10 rows)
        emz(db, 36, 10); emz(db, 36, 12) // Sentadilla de Sumo → Cuádriceps + Aductores
        emz(db, 38, 10); emz(db, 38, 15) // Sentadilla Búlgara Dividida → Cuádriceps + Glúteos
        emz(db, 40, 10); emz(db, 40, 15) // Subir Escalones → Cuádriceps + Glúteos
        emz(db, 41, 15); emz(db, 41, 10) // Zancada hacia atrás → Glúteos + Cuádriceps
        emz(db, 42, 10); emz(db, 42, 15) // Avanzada de Zancadas → Cuádriceps + Glúteos
    }

    private fun emz(db: SupportSQLiteDatabase, exerciseId: Long, muscleZoneId: Long) {
        val values = ContentValues().apply {
            put("exercise_id", exerciseId)
            put("muscle_zone_id", muscleZoneId)
        }
        db.insert("exercise_muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
