package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object PlanSeeder {

    private const val SETS = 4
    private const val R8_12 = "8-12"
    private const val FAILURE = "TO_TECHNICAL_FAILURE"
    private const val R30_45 = "30-45_SEC"

    fun seed(db: SupportSQLiteDatabase) {
        seedModuleVersions(db)
        seedPlanAssignments(db)
    }

    private fun seedModuleVersions(db: SupportSQLiteDatabase) {
        mv(db, 1, "A", 1); mv(db, 2, "A", 2); mv(db, 3, "A", 3)
        mv(db, 4, "B", 1); mv(db, 5, "B", 2); mv(db, 6, "B", 3)
        mv(db, 7, "C", 1); mv(db, 8, "C", 2); mv(db, 9, "C", 3)
    }

    private fun mv(db: SupportSQLiteDatabase, id: Long, moduleCode: String, versionNumber: Int) {
        val values = ContentValues().apply {
            put("id", id)
            put("module_code", moduleCode)
            put("version_number", versionNumber)
        }
        db.insert("module_version", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    @Suppress("LongMethod")
    private fun seedPlanAssignments(db: SupportSQLiteDatabase) {
        // ===== Module A — Pull + Abs =====
        // A-V1 (moduleVersionId = 1) — 11 exercises
        pa(db, 1, 10, R8_12); pa(db, 1, 8, R8_12);  pa(db, 1, 9, R8_12)
        pa(db, 1, 16, R8_12); pa(db, 1, 18, R8_12); pa(db, 1, 17, R8_12)
        pa(db, 1, 19, R8_12); pa(db, 1, 11, R8_12); pa(db, 1, 12, R8_12)
        pa(db, 1, 13, R8_12); pa(db, 1, 14, R30_45)

        // A-V2 (moduleVersionId = 2) — 11 exercises
        pa(db, 2, 10, R8_12); pa(db, 2, 8, R8_12);  pa(db, 2, 9, R8_12)
        pa(db, 2, 26, R8_12); pa(db, 2, 16, R8_12); pa(db, 2, 18, R8_12)
        pa(db, 2, 17, R8_12); pa(db, 2, 20, R8_12); pa(db, 2, 11, R8_12)
        pa(db, 2, 14, R30_45); pa(db, 2, 15, R30_45)

        // A-V3 (moduleVersionId = 3) — 11 exercises
        pa(db, 3, 10, R8_12); pa(db, 3, 8, R8_12);  pa(db, 3, 9, R8_12)
        pa(db, 3, 26, R8_12); pa(db, 3, 16, R8_12); pa(db, 3, 19, R8_12)
        pa(db, 3, 17, R8_12); pa(db, 3, 20, R8_12); pa(db, 3, 11, R8_12)
        pa(db, 3, 13, R8_12); pa(db, 3, 14, R30_45)

        // ===== Module B — Push =====
        // B-V1 (moduleVersionId = 4) — 11 exercises
        pa(db, 4, 1, R8_12);  pa(db, 4, 3, R8_12);  pa(db, 4, 6, R8_12)
        pa(db, 4, 4, FAILURE); pa(db, 4, 27, R8_12); pa(db, 4, 24, R8_12)
        pa(db, 4, 25, R8_12); pa(db, 4, 28, R8_12); pa(db, 4, 21, R8_12)
        pa(db, 4, 22, R8_12); pa(db, 4, 23, R8_12)

        // B-V2 (moduleVersionId = 5) — 11 exercises
        pa(db, 5, 1, R8_12);  pa(db, 5, 7, R8_12);  pa(db, 5, 6, R8_12)
        pa(db, 5, 5, R8_12);  pa(db, 5, 27, R8_12); pa(db, 5, 25, R8_12)
        pa(db, 5, 29, R8_12); pa(db, 5, 2, R8_12);  pa(db, 5, 21, R8_12)
        pa(db, 5, 22, R8_12); pa(db, 5, 23, R8_12)

        // B-V3 (moduleVersionId = 6) — 11 exercises
        pa(db, 6, 1, R8_12);  pa(db, 6, 3, R8_12);  pa(db, 6, 6, R8_12)
        pa(db, 6, 5, R8_12);  pa(db, 6, 27, R8_12); pa(db, 6, 24, R8_12)
        pa(db, 6, 28, R8_12); pa(db, 6, 2, R8_12);  pa(db, 6, 21, R8_12)
        pa(db, 6, 22, R8_12); pa(db, 6, 23, R8_12)

        // ===== Module C =====
        // C-V1 (moduleVersionId = 7) — 9 exercises
        pa(db, 7, 39, R8_12); pa(db, 7, 43, R8_12); pa(db, 7, 30, R8_12)
        pa(db, 7, 31, R8_12); pa(db, 7, 35, R8_12); pa(db, 7, 32, R8_12)
        pa(db, 7, 33, R8_12); pa(db, 7, 34, R8_12); pa(db, 7, 42, R8_12)

        // C-V2 (moduleVersionId = 8) — 9 exercises
        pa(db, 8, 36, R8_12); pa(db, 8, 38, R8_12); pa(db, 8, 43, R8_12)
        pa(db, 8, 30, R8_12); pa(db, 8, 31, R8_12); pa(db, 8, 41, R8_12)
        pa(db, 8, 33, R8_12); pa(db, 8, 34, R8_12); pa(db, 8, 42, R8_12)

        // C-V3 (moduleVersionId = 9) — 9 exercises
        pa(db, 9, 38, R8_12); pa(db, 9, 40, R8_12); pa(db, 9, 37, R8_12)
        pa(db, 9, 30, R8_12); pa(db, 9, 31, R8_12); pa(db, 9, 32, R8_12)
        pa(db, 9, 33, R8_12); pa(db, 9, 34, R8_12); pa(db, 9, 42, R8_12)
    }

    private fun pa(db: SupportSQLiteDatabase, moduleVersionId: Long, exerciseId: Long, reps: String) {
        val values = ContentValues().apply {
            put("module_version_id", moduleVersionId)
            put("exercise_id", exerciseId)
            put("sets", SETS)
            put("reps", reps)
        }
        db.insert("plan_assignment", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
