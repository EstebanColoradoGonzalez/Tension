package com.estebancoloradogonzalez.tension.data.local.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Update module metadata (A and B)
            db.execSQL(
                "UPDATE module SET name = 'Módulo A — Superior (Pull + Abs)', " +
                    "group_description = 'Espalda, Bíceps, Abdomen' WHERE code = 'A'",
            )
            db.execSQL(
                "UPDATE module SET name = 'Módulo B — Superior (Push)', " +
                    "group_description = 'Pecho, Hombro, Tríceps' WHERE code = 'B'",
            )

            // 2. Reassign Chest exercises (A → B)
            db.execSQL("UPDATE exercise SET module_code = 'B' WHERE id IN (1, 2, 3, 4, 5, 6, 7)")

            // 3. Reassign Biceps + Dumbbell Shrugs exercises (B → A)
            db.execSQL("UPDATE exercise SET module_code = 'A' WHERE id IN (16, 17, 18, 19, 20, 26)")

            // 4. Fix muscle zone of Dumbbell Shrugs (Hombro → Espalda Media)
            db.execSQL("UPDATE exercise_muscle_zone SET muscle_zone_id = 4 WHERE exercise_id = 26")

            // 5. Delete old plan assignments for modules A and B
            db.execSQL("DELETE FROM plan_assignment WHERE module_version_id IN (1, 2, 3, 4, 5, 6)")

            // 6. Insert new assignments for Module A (Pull + Abs)
            // A-V1
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 10, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 8, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 9, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 16, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 18, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 17, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 19, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 11, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 12, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 13, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (1, 14, 4, '30-45_SEC')")

            // A-V2
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 10, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 8, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 9, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 26, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 16, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 18, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 17, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 20, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 11, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 14, 4, '30-45_SEC')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (2, 15, 4, '30-45_SEC')")

            // A-V3
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 10, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 8, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 9, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 26, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 16, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 19, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 17, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 20, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 11, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 13, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (3, 14, 4, '30-45_SEC')")

            // 7. Insert new assignments for Module B (Push)
            // B-V1
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 1, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 3, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 6, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 4, 4, 'TO_TECHNICAL_FAILURE')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 27, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 24, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 25, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 28, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 21, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 22, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (4, 23, 4, '8-12')")

            // B-V2
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 1, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 7, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 6, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 5, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 27, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 25, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 29, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 2, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 21, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 22, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (5, 23, 4, '8-12')")

            // B-V3
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 1, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 3, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 6, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 5, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 27, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 24, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 28, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 2, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 21, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 22, 4, '8-12')")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps) VALUES (6, 23, 4, '8-12')")
        }
    }

    @Suppress("LongMethod")
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Add sort_order column to plan_assignment
            db.execSQL(
                "ALTER TABLE plan_assignment ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0",
            )

            // 2. Delete all current plan assignments
            db.execSQL("DELETE FROM plan_assignment")

            // 3. Insert 82 new assignments with sort_order
            // ===== Module A — Pull + Abs =====
            // A-V1 (moduleVersionId = 1) — 12 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 10, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 8, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 9, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 26, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 16, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 18, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 17, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 19, 4, '8-12', 8)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 11, 4, '8-12', 9)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 12, 4, '8-12', 10)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 13, 4, '8-12', 11)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (1, 14, 4, '30-45_SEC', 12)")

            // A-V2 (moduleVersionId = 2) — 11 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 10, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 8, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 9, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 26, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 16, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 18, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 17, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 20, 4, '8-12', 8)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 11, 4, '8-12', 9)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 14, 4, '30-45_SEC', 10)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (2, 15, 4, '30-45_SEC', 11)")

            // A-V3 (moduleVersionId = 3) — 11 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 10, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 8, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 9, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 26, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 16, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 19, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 17, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 20, 4, '8-12', 8)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 11, 4, '8-12', 9)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 13, 4, '8-12', 10)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (3, 14, 4, '30-45_SEC', 11)")

            // ===== Module B — Push =====
            // B-V1 (moduleVersionId = 4) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 1, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 3, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 6, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 27, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 25, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 24, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 22, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (4, 23, 4, '8-12', 8)")

            // B-V2 (moduleVersionId = 5) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 1, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 7, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 5, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 27, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 25, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 29, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 21, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (5, 23, 4, '8-12', 8)")

            // B-V3 (moduleVersionId = 6) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 1, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 2, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 4, 4, 'TO_TECHNICAL_FAILURE', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 27, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 24, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 28, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 21, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (6, 22, 4, '8-12', 8)")

            // ===== Module C — Pierna =====
            // C-V1 (moduleVersionId = 7) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 39, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 43, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 30, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 31, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 35, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 32, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 33, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (7, 34, 4, '8-12', 8)")

            // C-V2 (moduleVersionId = 8) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 36, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 38, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 43, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 30, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 31, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 41, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 33, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (8, 34, 4, '8-12', 8)")

            // C-V3 (moduleVersionId = 9) — 8 exercises
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 38, 4, '8-12', 1)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 40, 4, '8-12', 2)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 37, 4, '8-12', 3)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 30, 4, '8-12', 4)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 31, 4, '8-12', 5)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 32, 4, '8-12', 6)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 33, 4, '8-12', 7)")
            db.execSQL("INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (9, 34, 4, '8-12', 8)")
        }
    }

    @Suppress("LongMethod")
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ===== Phase 1: Create new tables =====
            db.execSQL(
                """
                CREATE TABLE routine (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    sort_order INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
                """,
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_routine_name ON routine(name)",
            )

            db.execSQL(
                """
                CREATE TABLE routine_version (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    routine_id INTEGER NOT NULL,
                    version_number INTEGER NOT NULL,
                    FOREIGN KEY(routine_id) REFERENCES routine(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """,
            )
            db.execSQL("CREATE INDEX `index_routine_version_routine_id` ON routine_version(routine_id)")
            db.execSQL("CREATE UNIQUE INDEX `index_routine_version_routine_id_version_number` ON routine_version(routine_id, version_number)")

            db.execSQL(
                """
                CREATE TABLE routine_current_version (
                    routine_id INTEGER NOT NULL,
                    current_version_number INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(routine_id),
                    FOREIGN KEY(routine_id) REFERENCES routine(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """,
            )

            db.execSQL(
                """
                CREATE TABLE deload_frozen_version (
                    deload_id INTEGER NOT NULL,
                    routine_id INTEGER NOT NULL,
                    frozen_version_number INTEGER NOT NULL,
                    PRIMARY KEY(deload_id, routine_id),
                    FOREIGN KEY(deload_id) REFERENCES deload(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(routine_id) REFERENCES routine(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """,
            )

            // ===== Phase 2: Populate from existing data (CA-35) =====
            db.execSQL(
                """
                INSERT INTO routine (id, name, sort_order, created_at)
                SELECT CASE m.code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 END,
                       m.name,
                       CASE m.code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 END,
                       date('now')
                FROM module m
                ORDER BY m.code
                """,
            )

            db.execSQL(
                """
                INSERT INTO routine_version (id, routine_id, version_number)
                SELECT mv.id,
                       CASE mv.module_code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 END,
                       mv.version_number
                FROM module_version mv
                """,
            )

            db.execSQL(
                """
                INSERT INTO routine_current_version (routine_id, current_version_number)
                SELECT 1, current_version_module_a FROM rotation_state WHERE id = 1
                UNION ALL
                SELECT 2, current_version_module_b FROM rotation_state WHERE id = 1
                UNION ALL
                SELECT 3, current_version_module_c FROM rotation_state WHERE id = 1
                """,
            )

            db.execSQL(
                """
                INSERT INTO deload_frozen_version (deload_id, routine_id, frozen_version_number)
                SELECT id, 1, frozen_version_module_a FROM deload
                UNION ALL
                SELECT id, 2, frozen_version_module_b FROM deload
                UNION ALL
                SELECT id, 3, frozen_version_module_c FROM deload
                """,
            )

            // ===== Phase 3: Migrate tables with FKs (recreate with new schema) =====

            // session: module_version_id → routine_version_id
            db.execSQL(
                """
                CREATE TABLE session_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    routine_version_id INTEGER NOT NULL,
                    deload_id INTEGER,
                    date TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                    FOREIGN KEY(routine_version_id) REFERENCES routine_version(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """,
            )
            db.execSQL("INSERT INTO session_new SELECT id, module_version_id, deload_id, date, status FROM session")
            db.execSQL("DROP TABLE session")
            db.execSQL("ALTER TABLE session_new RENAME TO session")
            db.execSQL("CREATE INDEX `index_session_date` ON session(date)")
            db.execSQL("CREATE INDEX `index_session_routine_version_id` ON session(routine_version_id)")
            db.execSQL("CREATE INDEX `index_session_status` ON session(status)")
            db.execSQL("CREATE INDEX `index_session_deload_id` ON session(deload_id)")

            // session_exercise: add original_exercise_id (nullable FK)
            db.execSQL(
                """
                CREATE TABLE session_exercise_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    original_exercise_id INTEGER,
                    progression_classification TEXT,
                    FOREIGN KEY(session_id) REFERENCES session(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(original_exercise_id) REFERENCES exercise(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """,
            )
            db.execSQL(
                """
                INSERT INTO session_exercise_new (id, session_id, exercise_id, original_exercise_id, progression_classification)
                SELECT id, session_id, exercise_id, NULL, progression_classification FROM session_exercise
                """,
            )
            db.execSQL("DROP TABLE session_exercise")
            db.execSQL("ALTER TABLE session_exercise_new RENAME TO session_exercise")
            db.execSQL("CREATE INDEX `index_session_exercise_session_id` ON session_exercise(session_id)")
            db.execSQL("CREATE INDEX `index_session_exercise_exercise_id` ON session_exercise(exercise_id)")
            db.execSQL("CREATE INDEX `index_session_exercise_original_exercise_id` ON session_exercise(original_exercise_id)")
            db.execSQL("CREATE UNIQUE INDEX `index_session_exercise_session_id_exercise_id` ON session_exercise(session_id, exercise_id)")

            // plan_assignment: module_version_id → routine_version_id
            db.execSQL(
                """
                CREATE TABLE plan_assignment_new (
                    routine_version_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    sets INTEGER NOT NULL,
                    reps TEXT NOT NULL,
                    sort_order INTEGER NOT NULL,
                    PRIMARY KEY(routine_version_id, exercise_id),
                    FOREIGN KEY(routine_version_id) REFERENCES routine_version(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """,
            )
            db.execSQL(
                "INSERT INTO plan_assignment_new SELECT module_version_id, exercise_id, sets, reps, sort_order FROM plan_assignment",
            )
            db.execSQL("DROP TABLE plan_assignment")
            db.execSQL("ALTER TABLE plan_assignment_new RENAME TO plan_assignment")
            db.execSQL("CREATE INDEX `index_plan_assignment_exercise_id` ON plan_assignment(exercise_id)")

            // exercise: remove module_code (exercises agnostic CA-15)
            db.execSQL(
                """
                CREATE TABLE exercise_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    equipment_type_id INTEGER NOT NULL,
                    is_bodyweight INTEGER NOT NULL DEFAULT 0,
                    is_isometric INTEGER NOT NULL DEFAULT 0,
                    is_to_technical_failure INTEGER NOT NULL DEFAULT 0,
                    is_custom INTEGER NOT NULL DEFAULT 0,
                    media_resource TEXT,
                    FOREIGN KEY(equipment_type_id) REFERENCES equipment_type(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """,
            )
            db.execSQL(
                """
                INSERT INTO exercise_new
                SELECT id, name, equipment_type_id, is_bodyweight, is_isometric,
                       is_to_technical_failure, is_custom, media_resource FROM exercise
                """,
            )
            db.execSQL("DROP TABLE exercise")
            db.execSQL("ALTER TABLE exercise_new RENAME TO exercise")
            db.execSQL("CREATE UNIQUE INDEX `index_exercise_name_equipment_type_id` ON exercise(name, equipment_type_id)")
            db.execSQL("CREATE INDEX `index_exercise_equipment_type_id` ON exercise(equipment_type_id)")

            // alert: module_code → routine_id
            db.execSQL(
                """
                CREATE TABLE alert_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    level TEXT NOT NULL,
                    exercise_id INTEGER,
                    routine_id INTEGER,
                    muscle_group TEXT,
                    message TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    resolved_at TEXT,
                    FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(routine_id) REFERENCES routine(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """,
            )
            db.execSQL(
                """
                INSERT INTO alert_new (id, type, level, exercise_id, routine_id, muscle_group,
                                       message, is_active, created_at, resolved_at)
                SELECT a.id, a.type, a.level, a.exercise_id,
                       CASE a.module_code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 ELSE NULL END,
                       a.muscle_group, a.message, a.is_active, a.created_at, a.resolved_at
                FROM alert a
                """,
            )
            db.execSQL("DROP TABLE alert")
            db.execSQL("ALTER TABLE alert_new RENAME TO alert")
            db.execSQL("CREATE INDEX `index_alert_is_active` ON alert(is_active)")
            db.execSQL("CREATE INDEX `index_alert_type` ON alert(type)")
            db.execSQL("CREATE INDEX `index_alert_exercise_id` ON alert(exercise_id)")
            db.execSQL("CREATE INDEX `index_alert_routine_id` ON alert(routine_id)")

            db.execSQL(
                "UPDATE alert SET type = 'ROUTINE_REQUIRES_DELOAD' WHERE type = 'MODULE_REQUIRES_DELOAD'",
            )

            // deload: remove frozen_version_module_a/b/c
            db.execSQL(
                """
                CREATE TABLE deload_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    status TEXT NOT NULL,
                    activation_date TEXT NOT NULL,
                    completion_date TEXT
                )
                """,
            )
            db.execSQL("INSERT INTO deload_new SELECT id, status, activation_date, completion_date FROM deload")
            db.execSQL("DROP TABLE deload")
            db.execSQL("ALTER TABLE deload_new RENAME TO deload")
            db.execSQL("CREATE INDEX `index_deload_status` ON deload(status)")

            // rotation_state: remove current_version_module_a/b/c
            db.execSQL(
                """
                CREATE TABLE rotation_state_new (
                    id INTEGER NOT NULL,
                    microcycle_position INTEGER NOT NULL,
                    microcycle_count INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """,
            )
            db.execSQL("INSERT INTO rotation_state_new SELECT id, microcycle_position, microcycle_count FROM rotation_state")
            db.execSQL("DROP TABLE rotation_state")
            db.execSQL("ALTER TABLE rotation_state_new RENAME TO rotation_state")

            // ===== Phase 4: Drop obsolete tables =====
            db.execSQL("DROP TABLE module_version")
            db.execSQL("DROP TABLE module")
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ===== Phase 1: New equipment types (CA-14) =====
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (10, 'Mancuernas o Polea')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (11, 'Polea con Cuerda')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (12, 'Polea con Cuerda o Polea con Barra en V')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (13, 'Barra')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (14, 'Mancuerna o Polea o Barra')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (15, 'Barra o Mancuernas')")

            // ===== Phase 2: Complete active deload if exists (CA-28) =====
            db.execSQL("UPDATE deload SET status = 'COMPLETED', completion_date = date('now') WHERE status = 'ACTIVE'")

            // ===== Phase 3: Close active session on seed versions (CA-22) =====
            db.execSQL(
                """
                UPDATE session SET status = 'COMPLETED'
                WHERE status = 'IN_PROGRESS'
                  AND routine_version_id IN (
                    SELECT id FROM routine_version WHERE routine_id IN (1, 2, 3)
                  )
                """,
            )

            // ===== Phase 4: Cleanup seed plan/routines (CA-17, CA-18, CA-19, CA-27) =====
            db.execSQL(
                """
                DELETE FROM plan_assignment
                WHERE routine_version_id IN (
                  SELECT id FROM routine_version WHERE routine_id IN (1, 2, 3)
                )
                """,
            )
            db.execSQL("DELETE FROM deload_frozen_version WHERE routine_id IN (1, 2, 3)")
            db.execSQL("DELETE FROM routine_current_version WHERE routine_id IN (1, 2, 3)")
            db.execSQL("DELETE FROM alert WHERE routine_id IN (1, 2, 3)")

            // Delete routine_versions that have NO sessions (CA-25)
            db.execSQL(
                """
                DELETE FROM routine_version
                WHERE routine_id IN (1, 2, 3)
                  AND id NOT IN (SELECT DISTINCT routine_version_id FROM session)
                """,
            )
            // Delete seed routines only if no versions remain (CA-25)
            db.execSQL(
                """
                DELETE FROM routine WHERE id IN (1, 2, 3)
                  AND id NOT IN (SELECT DISTINCT routine_id FROM routine_version)
                """,
            )

            // ===== Phase 5: Exercises — preserve with history, delete rest (CA-15, CA-23, CA-26) =====

            // Mark preconfigured exercises with history as custom (is_custom = 1)
            db.execSQL(
                """
                UPDATE exercise SET is_custom = 1
                WHERE is_custom = 0
                  AND id IN (
                    SELECT DISTINCT exercise_id FROM session_exercise
                    UNION
                    SELECT DISTINCT original_exercise_id FROM session_exercise
                    WHERE original_exercise_id IS NOT NULL
                  )
                """,
            )

            // Rename preserved exercises that would collide with new dictionary (UNIQUE name+equipment)
            val newExercises = mapOf(
                "Aductores" to 1L, "Cruce de Polea Alta" to 6L,
                "Crunch Abdominal" to 6L, "Curl Bayesian en Banco Inclinado" to 2L,
                "Curl de Concentración" to 5L, "Curl de Isquiotibiales Sentado" to 1L,
                "Curl de Martillo Cruzado" to 2L, "Curl de Predicador" to 5L,
                "Elevación de Pantorrilla en Máquina de Pie" to 1L,
                "Elevación Lateral" to 10L, "Extensión de Cuádriceps" to 1L,
                "Extensión de Tríceps en Polea (Pushdown)" to 12L,
                "Extensión de Tríceps por encima de la Cabeza" to 10L,
                "Face Pull" to 11L, "Hip Thrust" to 1L,
                "Peso Muerto Rumano" to 13L, "Prensa Inclinada" to 1L,
                "Press de Banca Inclinado" to 14L, "Press de Banca Plano" to 15L,
                "Press Pallof" to 6L, "Remo T Inclinado" to 1L,
                "Sentadilla Búlgara" to 2L, "Sentadilla de Zumo" to 5L,
                "Sentadilla Hack" to 1L, "Tirón de Dorsales" to 6L,
                "Vuelos Posteriores" to 2L,
            )
            for ((name, equipId) in newExercises) {
                db.execSQL(
                    """
                    UPDATE exercise SET name = name || ' (anterior)'
                    WHERE is_custom = 1 AND name = ? AND equipment_type_id = ?
                    """,
                    arrayOf(name, equipId),
                )
            }

            // Delete alerts for exercises without history (CA-27)
            db.execSQL(
                """
                DELETE FROM alert
                WHERE exercise_id IS NOT NULL
                  AND exercise_id IN (
                    SELECT id FROM exercise WHERE is_custom = 0
                  )
                """,
            )

            // Delete exercise_progression for exercises without history (CA-26)
            db.execSQL(
                """
                DELETE FROM exercise_progression
                WHERE exercise_id IN (
                  SELECT id FROM exercise WHERE is_custom = 0
                )
                """,
            )

            // Delete exercise_muscle_zone for exercises without history
            db.execSQL(
                """
                DELETE FROM exercise_muscle_zone
                WHERE exercise_id IN (
                  SELECT id FROM exercise WHERE is_custom = 0
                )
                """,
            )

            // Delete plan_assignment for exercises without history
            db.execSQL(
                """
                DELETE FROM plan_assignment
                WHERE exercise_id IN (
                  SELECT id FROM exercise WHERE is_custom = 0
                )
                """,
            )

            // Delete preconfigured exercises without history
            db.execSQL("DELETE FROM exercise WHERE is_custom = 0")

            // ===== Phase 6: Insert new exercises + plan (CA-03, CA-08-11, CA-16, CA-20, CA-21) =====
            val maxExId = db.query("SELECT COALESCE(MAX(id), 0) FROM exercise")
                .use { if (it.moveToFirst()) it.getLong(0) else 0L }
            val b = maxExId + 1 // base exercise ID

            insertNewExercise(db, b + 0, "Aductores", 1, "aductores_maquina")
            insertNewExercise(db, b + 1, "Cruce de Polea Alta", 6, "cruce_de_polea_alta_polea")
            insertNewExercise(db, b + 2, "Crunch Abdominal", 6, "crunch_abdominal_polea")
            insertNewExercise(db, b + 3, "Curl Bayesian en Banco Inclinado", 2, "curl_bayesian_en_banco_inclinado_mancuernas")
            insertNewExercise(db, b + 4, "Curl de Concentración", 5, "curl_de_concentracion_mancuerna")
            insertNewExercise(db, b + 5, "Curl de Isquiotibiales Sentado", 1, "curl_de_isquiotibiales_sentado_maquina")
            insertNewExercise(db, b + 6, "Curl de Martillo Cruzado", 2, "curl_de_martillo_cruzado_mancuernas")
            insertNewExercise(db, b + 7, "Curl de Predicador", 5, "curl_de_predicador_mancuerna")
            insertNewExercise(db, b + 8, "Elevación de Pantorrilla en Máquina de Pie", 1, "elevacion_de_pantorrilla_en_maquina_de_pie_maquina")
            insertNewExercise(db, b + 9, "Elevación Lateral", 10, "elevacion_lateral_mancuernas")
            insertNewExercise(db, b + 10, "Extensión de Cuádriceps", 1, "extension_de_cuadriceps_maquina")
            insertNewExercise(db, b + 11, "Extensión de Tríceps en Polea (Pushdown)", 12, "extension_de_triceps_en_polea_pushdown_polea_con_cuerda")
            insertNewExercise(db, b + 12, "Extensión de Tríceps por encima de la Cabeza", 10, "extension_de_triceps_por_encima_de_la_cabeza_mancuernas")
            insertNewExercise(db, b + 13, "Face Pull", 11, "face_pull_polea_con_cuerda")
            insertNewExercise(db, b + 14, "Hip Thrust", 1, "hip_thrust_maquina")
            insertNewExercise(db, b + 15, "Peso Muerto Rumano", 13, "peso_muerto_rumano_barra")
            insertNewExercise(db, b + 16, "Prensa Inclinada", 1, "prensa_inclinada_maquina")
            insertNewExercise(db, b + 17, "Press de Banca Inclinado", 14, "press_de_banca_inclinado_mancuerna")
            insertNewExercise(db, b + 18, "Press de Banca Plano", 15, "press_de_banca_plano_barra")
            insertNewExercise(db, b + 19, "Press Pallof", 6, "press_pallof_polea")
            insertNewExercise(db, b + 20, "Remo T Inclinado", 1, "remo_t_inclinado_maquina")
            insertNewExercise(db, b + 21, "Sentadilla Búlgara", 2, "sentadilla_bulgara_mancuernas")
            insertNewExercise(db, b + 22, "Sentadilla de Zumo", 5, "sentadilla_de_zumo_mancuerna")
            insertNewExercise(db, b + 23, "Sentadilla Hack", 1, "sentadilla_hack_maquina")
            insertNewExercise(db, b + 24, "Tirón de Dorsales", 6, "tiron_de_dorsales_polea")
            insertNewExercise(db, b + 25, "Vuelos Posteriores", 2, "vuelos_posteriores_mancuernas")

            // exercise_muscle_zone — single zone (23 exercises)
            insertEmz(db, b + 0, 12)   // Aductores → Aductores
            insertEmz(db, b + 1, 3)    // Cruce de Polea Alta → Pecho Inferior
            insertEmz(db, b + 2, 6)    // Crunch Abdominal → Abdomen
            insertEmz(db, b + 3, 9)    // Curl Bayesian → Bíceps
            insertEmz(db, b + 4, 9)    // Curl de Concentración → Bíceps
            insertEmz(db, b + 5, 11)   // Curl de Isquiotibiales → Isquiotibiales
            insertEmz(db, b + 6, 9)    // Curl de Martillo Cruzado → Bíceps
            insertEmz(db, b + 7, 9)    // Curl de Predicador → Bíceps
            insertEmz(db, b + 8, 14)   // Elevación de Pantorrilla → Gemelos
            insertEmz(db, b + 9, 7)    // Elevación Lateral → Hombro
            insertEmz(db, b + 10, 10)  // Extensión de Cuádriceps → Cuádriceps
            insertEmz(db, b + 11, 8)   // Extensión Tríceps Pushdown → Tríceps
            insertEmz(db, b + 12, 8)   // Extensión Tríceps Cabeza → Tríceps
            insertEmz(db, b + 13, 7)   // Face Pull → Hombro
            insertEmz(db, b + 14, 15)  // Hip Thrust → Glúteos
            insertEmz(db, b + 16, 10)  // Prensa Inclinada → Cuádriceps
            insertEmz(db, b + 17, 2)   // Press Banca Inclinado → Pecho Superior
            insertEmz(db, b + 18, 1)   // Press Banca Plano → Pecho Medio
            insertEmz(db, b + 19, 6)   // Press Pallof → Abdomen
            insertEmz(db, b + 20, 4)   // Remo T Inclinado → Espalda Media
            insertEmz(db, b + 23, 10)  // Sentadilla Hack → Cuádriceps
            insertEmz(db, b + 24, 5)   // Tirón de Dorsales → Dorsal Ancho
            insertEmz(db, b + 25, 7)   // Vuelos Posteriores → Hombro

            // exercise_muscle_zone — multi zone (3 exercises)
            insertEmz(db, b + 15, 11); insertEmz(db, b + 15, 15)  // Peso Muerto Rumano → Isquiotibiales + Glúteos
            insertEmz(db, b + 21, 10); insertEmz(db, b + 21, 15)  // Sentadilla Búlgara → Cuádriceps + Glúteos
            insertEmz(db, b + 22, 10); insertEmz(db, b + 22, 12)  // Sentadilla de Zumo → Cuádriceps + Aductores

            // New routines (CA-08)
            val maxRoutineId = db.query("SELECT COALESCE(MAX(id), 0) FROM routine")
                .use { if (it.moveToFirst()) it.getLong(0) else 0L }
            val piernaId = maxRoutineId + 1
            val pushId = maxRoutineId + 2
            val pullId = maxRoutineId + 3

            insertRoutine(db, piernaId, "Pierna (Leg)", 1)
            insertRoutine(db, pushId, "Pecho, Hombro, Tríceps (Push)", 2)
            insertRoutine(db, pullId, "Espalda, Bíceps y Abdomen (Pull)", 3)

            // New routine versions (CA-09)
            val maxRvId = db.query("SELECT COALESCE(MAX(id), 0) FROM routine_version")
                .use { if (it.moveToFirst()) it.getLong(0) else 0L }
            val rv1 = maxRvId + 1 // Pierna V1
            val rv2 = maxRvId + 2 // Pierna V2
            val rv3 = maxRvId + 3 // Push V1
            val rv4 = maxRvId + 4 // Pull V1

            insertRoutineVersion(db, rv1, piernaId, 1)
            insertRoutineVersion(db, rv2, piernaId, 2)
            insertRoutineVersion(db, rv3, pushId, 1)
            insertRoutineVersion(db, rv4, pullId, 1)

            // routine_current_version (CA-20)
            insertRcv(db, piernaId, 1)
            insertRcv(db, pushId, 1)
            insertRcv(db, pullId, 1)

            // Plan assignments (CA-11) — all sets=4, reps='8-12'
            // Pierna V1 (6 exercises)
            insertPa(db, rv1, b + 0, 1)   // Aductores
            insertPa(db, rv1, b + 5, 2)   // Curl de Isquiotibiales Sentado
            insertPa(db, rv1, b + 16, 3)  // Prensa Inclinada
            insertPa(db, rv1, b + 23, 4)  // Sentadilla Hack
            insertPa(db, rv1, b + 10, 5)  // Extensión de Cuádriceps
            insertPa(db, rv1, b + 8, 6)   // Elevación de Pantorrilla

            // Pierna V2 (6 exercises)
            insertPa(db, rv2, b + 0, 1)   // Aductores
            insertPa(db, rv2, b + 5, 2)   // Curl de Isquiotibiales Sentado
            insertPa(db, rv2, b + 15, 3)  // Peso Muerto Rumano
            insertPa(db, rv2, b + 14, 4)  // Hip Thrust
            insertPa(db, rv2, b + 10, 5)  // Extensión de Cuádriceps
            insertPa(db, rv2, b + 8, 6)   // Elevación de Pantorrilla

            // Push V1 (7 exercises)
            insertPa(db, rv3, b + 9, 1)   // Elevación Lateral
            insertPa(db, rv3, b + 17, 2)  // Press de Banca Inclinado
            insertPa(db, rv3, b + 18, 3)  // Press de Banca Plano
            insertPa(db, rv3, b + 25, 4)  // Vuelos Posteriores
            insertPa(db, rv3, b + 12, 5)  // Extensión Tríceps Cabeza
            insertPa(db, rv3, b + 1, 6)   // Cruce de Polea Alta
            insertPa(db, rv3, b + 11, 7)  // Extensión Tríceps Pushdown

            // Pull V1 (8 exercises)
            insertPa(db, rv4, b + 4, 1)   // Curl de Concentración
            insertPa(db, rv4, b + 24, 2)  // Tirón de Dorsales
            insertPa(db, rv4, b + 20, 3)  // Remo T Inclinado
            insertPa(db, rv4, b + 13, 4)  // Face Pull
            insertPa(db, rv4, b + 3, 5)   // Curl Bayesian en Banco Inclinado
            insertPa(db, rv4, b + 6, 6)   // Curl de Martillo Cruzado
            insertPa(db, rv4, b + 2, 7)   // Crunch Abdominal
            insertPa(db, rv4, b + 19, 8)  // Press Pallof

            // Reset rotation (CA-21)
            db.execSQL("UPDATE rotation_state SET microcycle_position = 1 WHERE id = 1")
        }

        private fun insertNewExercise(
            db: SupportSQLiteDatabase,
            id: Long,
            name: String,
            equipmentTypeId: Long,
            media: String,
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
            db.insert("exercise", SQLiteDatabase.CONFLICT_ABORT, values)
        }

        private fun insertEmz(db: SupportSQLiteDatabase, exerciseId: Long, muscleZoneId: Long) {
            val values = ContentValues().apply {
                put("exercise_id", exerciseId)
                put("muscle_zone_id", muscleZoneId)
            }
            db.insert("exercise_muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
        }

        private fun insertRoutine(db: SupportSQLiteDatabase, id: Long, name: String, sortOrder: Int) {
            val values = ContentValues().apply {
                put("id", id)
                put("name", name)
                put("sort_order", sortOrder)
                put("created_at", "2025-01-01")
            }
            db.insert("routine", SQLiteDatabase.CONFLICT_ABORT, values)
        }

        private fun insertRoutineVersion(
            db: SupportSQLiteDatabase,
            id: Long,
            routineId: Long,
            versionNumber: Int,
        ) {
            val values = ContentValues().apply {
                put("id", id)
                put("routine_id", routineId)
                put("version_number", versionNumber)
            }
            db.insert("routine_version", SQLiteDatabase.CONFLICT_ABORT, values)
        }

        private fun insertRcv(db: SupportSQLiteDatabase, routineId: Long, versionNumber: Int) {
            val values = ContentValues().apply {
                put("routine_id", routineId)
                put("current_version_number", versionNumber)
            }
            db.insert("routine_current_version", SQLiteDatabase.CONFLICT_ABORT, values)
        }

        private fun insertPa(
            db: SupportSQLiteDatabase,
            routineVersionId: Long,
            exerciseId: Long,
            sortOrder: Int,
        ) {
            val values = ContentValues().apply {
                put("routine_version_id", routineVersionId)
                put("exercise_id", exerciseId)
                put("sets", 4)
                put("reps", "8-12")
                put("sort_order", sortOrder)
            }
            db.insert("plan_assignment", SQLiteDatabase.CONFLICT_ABORT, values)
        }
    }

    @Suppress("LongMethod")
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ===== Phase 1: Add is_finalized column to session_exercise =====
            db.execSQL(
                "ALTER TABLE session_exercise ADD COLUMN is_finalized INTEGER NOT NULL DEFAULT 0",
            )

            // Backfill: mark all exercises in closed sessions as finalized
            db.execSQL(
                """
                UPDATE session_exercise
                SET is_finalized = 1
                WHERE session_id IN (
                    SELECT id FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')
                )
                """,
            )

            // Backfill: mark exercises in active session that have completedSets >= prescribedSets
            db.execSQL(
                """
                UPDATE session_exercise
                SET is_finalized = 1
                WHERE session_id IN (
                    SELECT id FROM session WHERE status = 'IN_PROGRESS'
                )
                AND (
                    SELECT COUNT(*) FROM exercise_set
                    WHERE exercise_set.session_exercise_id = session_exercise.id
                ) >= COALESCE(
                    (SELECT pa.sets FROM plan_assignment pa
                     INNER JOIN session s ON s.id = session_exercise.session_id
                     WHERE pa.routine_version_id = s.routine_version_id
                       AND pa.exercise_id = COALESCE(session_exercise.original_exercise_id, session_exercise.exercise_id)),
                    4
                )
                """,
            )

            // ===== Phase 2: Insert 5 new muscle zones (IDs 16-20) =====
            db.execSQL("INSERT INTO muscle_zone (id, name, muscle_group) VALUES (16, 'Espalda Alta', 'Espalda')")
            db.execSQL("INSERT INTO muscle_zone (id, name, muscle_group) VALUES (17, 'Trapecio', 'Espalda')")
            db.execSQL("INSERT INTO muscle_zone (id, name, muscle_group) VALUES (18, 'Espalda Baja', 'Espalda')")
            db.execSQL("INSERT INTO muscle_zone (id, name, muscle_group) VALUES (19, 'Antebrazo', 'Antebrazo')")
            db.execSQL("INSERT INTO muscle_zone (id, name, muscle_group) VALUES (20, 'Cuello', 'Cuello')")

            // ===== Phase 3: Insert 8 new equipment types (IDs 16-23) =====
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (16, 'Banda Elástica')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (17, 'Kettlebell')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (18, 'Barra EZ')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (19, 'TRX/Suspensión')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (20, 'Balón Medicinal')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (21, 'Rodillo de Abdomen')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (22, 'Paralelas/Dip Station')")
            db.execSQL("INSERT INTO equipment_type (id, name) VALUES (23, 'Barra Fija')")

            // ===== Phase 4: Reclassify Face Pull from Hombro(7) to Espalda Alta(16) =====
            db.execSQL(
                """
                UPDATE exercise_muscle_zone
                SET muscle_zone_id = 16
                WHERE exercise_id IN (SELECT id FROM exercise WHERE name = 'Face Pull')
                  AND muscle_zone_id = 7
                """,
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ===== Phase 1: Add slot column to plan_assignment =====
            // slot groups alternative exercises for the same position in the plan.
            // Existing exercises keep their individual slot = sort_order.
            db.execSQL(
                "ALTER TABLE plan_assignment ADD COLUMN slot INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "UPDATE plan_assignment SET slot = sort_order",
            )

            // ===== Phase 2: Add pending_selection and slot to session_exercise =====
            // pending_selection = 1 means the user must choose an exercise for this slot
            // before registering sets. slot links to plan_assignment.slot.
            db.execSQL(
                "ALTER TABLE session_exercise ADD COLUMN pending_selection INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE session_exercise ADD COLUMN slot INTEGER NOT NULL DEFAULT 0",
            )

            // ===== Phase 3: Make exercise_id nullable in session_exercise =====
            // SQLite does not support ALTER COLUMN, so recreate the table.
            db.execSQL(
                """
                CREATE TABLE session_exercise_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER,
                    original_exercise_id INTEGER,
                    progression_classification TEXT,
                    is_finalized INTEGER NOT NULL DEFAULT 0,
                    pending_selection INTEGER NOT NULL DEFAULT 0,
                    slot INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
                    FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT,
                    FOREIGN KEY(original_exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT
                )
                """,
            )
            db.execSQL(
                """
                INSERT INTO session_exercise_new (id, session_id, exercise_id, original_exercise_id,
                    progression_classification, is_finalized, pending_selection, slot)
                SELECT id, session_id, exercise_id, original_exercise_id,
                    progression_classification, is_finalized, 0, sort_order_proxy
                FROM (
                    SELECT se.*,
                        COALESCE(
                            (SELECT pa.sort_order FROM plan_assignment pa
                             INNER JOIN session s ON s.id = se.session_id
                             WHERE pa.routine_version_id = s.routine_version_id
                               AND pa.exercise_id = COALESCE(se.original_exercise_id, se.exercise_id)
                             LIMIT 1),
                            0
                        ) AS sort_order_proxy
                    FROM session_exercise se
                )
                """,
            )
            db.execSQL("DROP TABLE session_exercise")
            db.execSQL("ALTER TABLE session_exercise_new RENAME TO session_exercise")
            db.execSQL("CREATE INDEX index_session_exercise_session_id ON session_exercise(session_id)")
            db.execSQL("CREATE INDEX index_session_exercise_exercise_id ON session_exercise(exercise_id)")
            db.execSQL("CREATE INDEX index_session_exercise_original_exercise_id ON session_exercise(original_exercise_id)")
            db.execSQL("CREATE UNIQUE INDEX index_session_exercise_session_id_exercise_id ON session_exercise(session_id, exercise_id)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Fix slot values for fresh installs where the seeder did not set slot.
            db.execSQL("UPDATE plan_assignment SET slot = sort_order WHERE slot = 0")

            // Fix session_exercise.slot for active sessions created when plan_assignment
            // had slot = 0. Look up the correct slot from the now-fixed plan_assignment.
            db.execSQL(
                """
                UPDATE session_exercise
                SET slot = COALESCE(
                    (SELECT pa.slot
                     FROM plan_assignment pa
                     INNER JOIN session s ON s.id = session_exercise.session_id
                     WHERE pa.routine_version_id = s.routine_version_id
                       AND pa.exercise_id = COALESCE(session_exercise.original_exercise_id, session_exercise.exercise_id)
                     LIMIT 1),
                    slot
                )
                WHERE slot = 0
                """,
            )
        }
    }
}
