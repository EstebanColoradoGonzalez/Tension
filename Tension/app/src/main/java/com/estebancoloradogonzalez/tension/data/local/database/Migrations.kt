package com.estebancoloradogonzalez.tension.data.local.database

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
}
