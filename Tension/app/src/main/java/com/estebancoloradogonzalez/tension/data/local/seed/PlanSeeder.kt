package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object PlanSeeder {

    private const val R8_12 = "8-12"

    fun seed(db: SupportSQLiteDatabase) {
        seedRoutines(db)
        seedRoutineVersions(db)
        seedRoutineCurrentVersions(db)
        seedPlanAssignments(db)
    }

    private fun seedRoutines(db: SupportSQLiteDatabase) {
        routine(db, 1, "Lunes: Pecho y Hombro (Push - Foco Deltoides Lateral)", 1)
        routine(db, 2, "Martes: Espalda, Bíceps y Abdomen (Pull - Foco Dorsal Ancho)", 2)
        routine(db, 3, "Miércoles: Pierna (Lower - Foco Cuádriceps)", 3)
        routine(db, 4, "Jueves: Pecho y Tríceps (Push - Foco Tríceps)", 4)
        routine(db, 5, "Viernes: Espalda, Bíceps y Abdomen (Pull - Foco Espalda Alta)", 5)
        routine(db, 6, "Sábado: Pierna (Lower - Foco Isquiotibiales)", 6)
    }

    private fun routine(db: SupportSQLiteDatabase, id: Long, name: String, sortOrder: Int) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("sort_order", sortOrder)
            put("created_at", "2025-01-01")
        }
        db.insert("routine", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun seedRoutineVersions(db: SupportSQLiteDatabase) {
        rv(db, 1, 1, 1)
        rv(db, 2, 2, 1)
        rv(db, 3, 3, 1)
        rv(db, 4, 4, 1)
        rv(db, 5, 5, 1)
        rv(db, 6, 6, 1)
    }

    private fun rv(db: SupportSQLiteDatabase, id: Long, routineId: Long, versionNumber: Int) {
        val values = ContentValues().apply {
            put("id", id)
            put("routine_id", routineId)
            put("version_number", versionNumber)
        }
        db.insert("routine_version", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun seedRoutineCurrentVersions(db: SupportSQLiteDatabase) {
        rcv(db, 1, 1)
        rcv(db, 2, 1)
        rcv(db, 3, 1)
        rcv(db, 4, 1)
        rcv(db, 5, 1)
        rcv(db, 6, 1)
    }

    private fun rcv(db: SupportSQLiteDatabase, routineId: Long, currentVersionNumber: Int) {
        val values = ContentValues().apply {
            put("routine_id", routineId)
            put("current_version_number", currentVersionNumber)
        }
        db.insert("routine_current_version", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    @Suppress("LongMethod")
    private fun seedPlanAssignments(db: SupportSQLiteDatabase) {
        // ===== Rutina 1 — Push Foco Deltoides Lateral (Lunes) =====
        pa(db, 1, 10, R8_12, 1, sets = 4)  // Elevación Lateral
        pa(db, 1, 18, R8_12, 2, sets = 3)  // Press de Banca Inclinado
        pa(db, 1, 19, R8_12, 3, sets = 3)  // Press de Banca Plano
        pa(db, 1, 27, R8_12, 4, sets = 3)  // Remo al Mentón
        pa(db, 1, 28, R8_12, 5, sets = 3)  // Aperturas

        // ===== Rutina 2 — Pull Foco Dorsal Ancho (Martes) =====
        pa(db, 2, 25, R8_12, 1, sets = 4)  // Tirón de Dorsales
        pa(db, 2, 29, R8_12, 2, sets = 3)  // Pull-Over
        pa(db, 2, 30, R8_12, 3, sets = 3)  // Curl Martillo
        pa(db, 2, 4,  R8_12, 4, sets = 3)  // Curl Bayesian en Banco Inclinado (Curl Inclinado/Bayesian)
        pa(db, 2, 3,  R8_12, 5, sets = 3)  // Crunch Abdominal

        // ===== Rutina 3 — Lower Foco Cuádriceps (Miércoles) =====
        pa(db, 3, 1,  R8_12, 1, sets = 3)            // Aductores
        pa(db, 3, 11, R8_12, 2, sets = 4)            // Extensión de Cuádriceps
        pa(db, 3, 24, R8_12, 3, sets = 3)            // Sentadilla Hack (primario slot 3)
        pa(db, 3, 17, R8_12, 4, sets = 3, slot = 3)  // Prensa Inclinada (alternativa slot 3)
        pa(db, 3, 22, R8_12, 5, sets = 3, slot = 4)  // Sentadilla Búlgara
        pa(db, 3, 9,  R8_12, 6, sets = 3, slot = 5)  // Elevación de Pantorrilla

        // ===== Rutina 4 — Push Foco Tríceps (Jueves) =====
        pa(db, 4, 13, R8_12, 1, sets = 4)  // Extensión de Tríceps por encima de la Cabeza
        pa(db, 4, 19, R8_12, 2, sets = 3)  // Press de Banca Plano
        pa(db, 4, 28, R8_12, 3, sets = 3)  // Aperturas
        pa(db, 4, 12, R8_12, 4, sets = 3)  // Extensión de Tríceps en Polea (Pushdown)
        pa(db, 4, 31, R8_12, 5, sets = 3)  // Rompecráneos

        // ===== Rutina 5 — Pull Foco Espalda Alta (Viernes) =====
        pa(db, 5, 21, R8_12, 1, sets = 4)            // Remo T Inclinado
        pa(db, 5, 32, R8_12, 2, sets = 3)            // Remo Horizontal
        pa(db, 5, 14, R8_12, 3, sets = 3)            // Face Pull (primario slot 3)
        pa(db, 5, 26, R8_12, 4, sets = 3, slot = 3)  // Vuelos Posteriores (alternativa slot 3)
        pa(db, 5, 8,  R8_12, 5, sets = 3, slot = 4)  // Curl de Predicador
        pa(db, 5, 3,  R8_12, 6, sets = 3, slot = 5)  // Crunch Abdominal

        // ===== Rutina 6 — Lower Foco Isquiotibiales (Sábado) =====
        pa(db, 6, 1,  R8_12, 1, sets = 3)  // Aductores
        pa(db, 6, 6,  R8_12, 2, sets = 4)  // Curl de Isquiotibiales Sentado
        pa(db, 6, 16, R8_12, 3, sets = 3)  // Peso Muerto Rumano
        pa(db, 6, 33, R8_12, 4, sets = 3)  // Zancadas
        pa(db, 6, 11, R8_12, 5, sets = 3)  // Extensión de Cuádriceps
        pa(db, 6, 9,  R8_12, 6, sets = 3)  // Elevación de Pantorrilla
    }

    private fun pa(
        db: SupportSQLiteDatabase,
        routineVersionId: Long,
        exerciseId: Long,
        reps: String,
        sortOrder: Int,
        sets: Int,
        slot: Int = sortOrder,
    ) {
        val values = ContentValues().apply {
            put("routine_version_id", routineVersionId)
            put("exercise_id", exerciseId)
            put("sets", sets)
            put("reps", reps)
            put("sort_order", sortOrder)
            put("slot", slot)
        }
        db.insert("plan_assignment", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
