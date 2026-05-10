package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object PlanSeeder {

    private const val SETS = 4
    private const val R8_12 = "8-12"

    fun seed(db: SupportSQLiteDatabase) {
        seedRoutines(db)
        seedRoutineVersions(db)
        seedRoutineCurrentVersions(db)
        seedPlanAssignments(db)
    }

    private fun seedRoutines(db: SupportSQLiteDatabase) {
        routine(db, 1, "Pierna (Leg)", 3)
        routine(db, 2, "Pecho, Hombro y Tríceps (Push)", 1)
        routine(db, 3, "Espalda, Bíceps y Abdomen (Pull)", 2)
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
        rv(db, 1, 1, 1)  // Pierna V1
        rv(db, 2, 1, 2)  // Pierna V2
        rv(db, 3, 2, 1)  // Push V1
        rv(db, 4, 3, 1)  // Pull V1
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
        // ===== Routine 1 — Pierna (Leg) =====
        // V1 (routineVersionId = 1) — 6 exercises
        pa(db, 1, 1, R8_12, 1)   // Aductores
        pa(db, 1, 6, R8_12, 2)   // Curl de Isquiotibiales Sentado
        pa(db, 1, 17, R8_12, 3)  // Prensa Inclinada
        pa(db, 1, 24, R8_12, 4)  // Sentadilla Hack
        pa(db, 1, 11, R8_12, 5)  // Extensión de Cuádriceps
        pa(db, 1, 9, R8_12, 6)   // Elevación de Pantorrilla en Máquina de Pie

        // V2 (routineVersionId = 2) — 6 slots, 8 exercises (slot 4 has 3 alternatives)
        pa(db, 2, 1, R8_12, 1)            // Aductores
        pa(db, 2, 6, R8_12, 2)            // Curl de Isquiotibiales Sentado
        pa(db, 2, 16, R8_12, 3)           // Peso Muerto Rumano
        pa(db, 2, 15, R8_12, 4)           // Hip Thrust (Máquina)
        pa(db, 2, 22, R8_12, 5, slot = 4) // Sentadilla Búlgara (Mancuernas) — alternativa slot 4
        pa(db, 2, 23, R8_12, 6, slot = 4) // Sentadilla de Zumo (Mancuerna) — alternativa slot 4
        pa(db, 2, 11, R8_12, 7, slot = 5) // Extensión de Cuádriceps
        pa(db, 2, 9, R8_12, 8, slot = 6)  // Elevación de Pantorrilla en Máquina de Pie

        // ===== Routine 2 — Pecho, Hombro, Tríceps (Push) =====
        // V1 (routineVersionId = 3) — 7 exercises
        pa(db, 3, 10, R8_12, 1)  // Elevación Lateral
        pa(db, 3, 18, R8_12, 2)  // Press de Banca Inclinado
        pa(db, 3, 19, R8_12, 3)  // Press de Banca Plano
        pa(db, 3, 26, R8_12, 4)  // Vuelos Posteriores
        pa(db, 3, 13, R8_12, 5)  // Extensión de Tríceps por encima de la Cabeza
        pa(db, 3, 2, R8_12, 6)   // Cruce de Polea Alta
        pa(db, 3, 12, R8_12, 7)  // Extensión de Tríceps en Polea (Pushdown)

        // ===== Routine 3 — Espalda, Bíceps y Abdomen (Pull) =====
        // V1 (routineVersionId = 4) — 8 slots, 9 exercises (slot 1 has 2 alternatives)
        pa(db, 4, 5, R8_12, 1)            // Curl de Concentración
        pa(db, 4, 8, R8_12, 2, slot = 1)  // Curl de Predicador (Mancuerna) — alternativa slot 1
        pa(db, 4, 25, R8_12, 3, slot = 2) // Tirón de Dorsales
        pa(db, 4, 21, R8_12, 4, slot = 3) // Remo T Inclinado
        pa(db, 4, 14, R8_12, 5, slot = 4) // Face Pull
        pa(db, 4, 4, R8_12, 6, slot = 5)  // Curl Bayesian en Banco Inclinado
        pa(db, 4, 7, R8_12, 7, slot = 6)  // Curl de Martillo Cruzado
        pa(db, 4, 3, R8_12, 8, slot = 7)  // Crunch Abdominal
        pa(db, 4, 20, R8_12, 9, slot = 8) // Press Pallof
    }

    private fun pa(
        db: SupportSQLiteDatabase,
        routineVersionId: Long,
        exerciseId: Long,
        reps: String,
        sortOrder: Int,
        slot: Int = sortOrder,
    ) {
        val values = ContentValues().apply {
            put("routine_version_id", routineVersionId)
            put("exercise_id", exerciseId)
            put("sets", SETS)
            put("reps", reps)
            put("sort_order", sortOrder)
            put("slot", slot)
        }
        db.insert("plan_assignment", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
