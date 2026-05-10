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
        ex(db, 1, "Aductores", 1, media = "aductores_maquina")
        ex(db, 2, "Cruce de Polea Alta", 6, media = "cruce_de_polea_alta_polea")
        ex(db, 3, "Crunch Abdominal", 6, media = "crunch_abdominal_polea")
        ex(db, 4, "Curl Bayesian en Banco Inclinado", 2, media = "curl_bayesian_en_banco_inclinado_mancuernas")
        ex(db, 5, "Curl de Concentración", 5, media = "curl_de_concentracion_mancuerna")
        ex(db, 6, "Curl de Isquiotibiales Sentado", 1, media = "curl_de_isquiotibiales_sentado_maquina")
        ex(db, 7, "Curl de Martillo Cruzado", 2, media = "curl_de_martillo_cruzado_mancuernas")
        ex(db, 8, "Curl de Predicador", 5, media = "curl_de_predicador_mancuerna")
        ex(db, 9, "Elevación de Pantorrilla en Máquina de Pie", 1, media = "elevacion_de_pantorrilla_en_maquina_de_pie_maquina")
        ex(db, 10, "Elevación Lateral", 10, media = "elevacion_lateral_mancuernas")
        ex(db, 11, "Extensión de Cuádriceps", 1, media = "extension_de_cuadriceps_maquina")
        ex(db, 12, "Extensión de Tríceps en Polea (Pushdown)", 12, media = "extension_de_triceps_en_polea_pushdown_polea_con_cuerda")
        ex(db, 13, "Extensión de Tríceps por encima de la Cabeza", 10, media = "extension_de_triceps_por_encima_de_la_cabeza_mancuernas")
        ex(db, 14, "Face Pull", 11, media = "face_pull_polea_con_cuerda")
        ex(db, 15, "Hip Thrust", 1, media = "hip_thrust_maquina")
        ex(db, 16, "Peso Muerto Rumano", 13, media = "peso_muerto_rumano_barra")
        ex(db, 17, "Prensa Inclinada", 1, media = "prensa_inclinada_maquina")
        ex(db, 18, "Press de Banca Inclinado", 14, media = "press_de_banca_inclinado_mancuerna")
        ex(db, 19, "Press de Banca Plano", 15, media = "press_de_banca_plano_barra")
        ex(db, 20, "Press Pallof", 6, media = "press_pallof_polea")
        ex(db, 21, "Remo T Inclinado", 1, media = "remo_t_inclinado_maquina")
        ex(db, 22, "Sentadilla Búlgara", 2, media = "sentadilla_bulgara_mancuernas")
        ex(db, 23, "Sentadilla de Zumo", 5, media = "sentadilla_de_zumo_mancuerna")
        ex(db, 24, "Sentadilla Hack", 1, media = "sentadilla_hack_maquina")
        ex(db, 25, "Tirón de Dorsales", 6, media = "tiron_de_dorsales_polea")
        ex(db, 26, "Vuelos Posteriores", 2, media = "vuelos_posteriores_mancuernas")
    }

    private fun ex(
        db: SupportSQLiteDatabase,
        id: Long,
        name: String,
        equipmentTypeId: Long,
        media: String? = null,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("equipment_type_id", equipmentTypeId)
            put("is_bodyweight", 0)
            put("is_isometric", 0)
            put("is_to_technical_failure", 0)
            put("is_custom", 0)
            put("media_resource", media)
        }
        db.insert("exercise", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    @Suppress("LongMethod")
    private fun seedExerciseMuscleZones(db: SupportSQLiteDatabase) {
        // Single-zone exercises
        emz(db, 1, 12)   // Aductores → Aductores
        emz(db, 2, 3)    // Cruce de Polea Alta → Pecho Inferior
        emz(db, 3, 6)    // Crunch Abdominal → Abdomen
        emz(db, 4, 9)    // Curl Bayesian en Banco Inclinado → Bíceps
        emz(db, 5, 9)    // Curl de Concentración → Bíceps
        emz(db, 6, 11)   // Curl de Isquiotibiales Sentado → Isquiotibiales
        emz(db, 7, 9)    // Curl de Martillo Cruzado → Bíceps
        emz(db, 8, 9)    // Curl de Predicador → Bíceps
        emz(db, 9, 14)   // Elevación de Pantorrilla → Gemelos
        emz(db, 10, 7)   // Elevación Lateral → Hombro
        emz(db, 11, 10)  // Extensión de Cuádriceps → Cuádriceps
        emz(db, 12, 8)   // Extensión de Tríceps Pushdown → Tríceps
        emz(db, 13, 8)   // Extensión de Tríceps Cabeza → Tríceps
        emz(db, 14, 16)   // Face Pull → Espalda Alta
        emz(db, 15, 15)  // Hip Thrust → Glúteos
        emz(db, 17, 10)  // Prensa Inclinada → Cuádriceps
        emz(db, 18, 2)   // Press de Banca Inclinado → Pecho Superior
        emz(db, 19, 1)   // Press de Banca Plano → Pecho Medio
        emz(db, 20, 6)   // Press Pallof → Abdomen
        emz(db, 21, 4)   // Remo T Inclinado → Espalda Media
        emz(db, 24, 10)  // Sentadilla Hack → Cuádriceps
        emz(db, 25, 5)   // Tirón de Dorsales → Dorsal Ancho
        emz(db, 26, 7)   // Vuelos Posteriores → Hombro

        // Multi-zone exercises
        emz(db, 16, 11); emz(db, 16, 15) // Peso Muerto Rumano → Isquiotibiales + Glúteos
        emz(db, 22, 10); emz(db, 22, 15) // Sentadilla Búlgara → Cuádriceps + Glúteos
        emz(db, 23, 10); emz(db, 23, 12) // Sentadilla de Zumo → Cuádriceps + Aductores
    }

    private fun emz(db: SupportSQLiteDatabase, exerciseId: Long, muscleZoneId: Long) {
        val values = ContentValues().apply {
            put("exercise_id", exerciseId)
            put("muscle_zone_id", muscleZoneId)
        }
        db.insert("exercise_muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
