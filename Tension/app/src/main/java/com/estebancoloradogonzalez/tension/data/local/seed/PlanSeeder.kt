package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedAssignment
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedRoutine

/**
 * Inserta el plan de entrenamiento predeterminado.
 *
 * Los datos residen en [DefaultPlan]; aquí solo se mapean a `ContentValues`. Cada rutina
 * genera una única versión (`version_number = 1`) que queda marcada como versión vigente.
 */
object PlanSeeder {

    private const val SEED_CREATED_AT = "2025-01-01"
    private const val FIRST_VERSION = 1

    fun seed(db: SupportSQLiteDatabase) {
        DefaultPlan.ROUTINES.forEach { routine ->
            insertRoutine(db, routine)
            insertRoutineVersion(db, routine.id)
            insertRoutineCurrentVersion(db, routine.id)
        }
        DefaultPlan.ASSIGNMENTS.forEach { assignment -> insertPlanAssignment(db, assignment) }
    }

    private fun insertRoutine(db: SupportSQLiteDatabase, routine: SeedRoutine) {
        val values = ContentValues().apply {
            put("id", routine.id)
            put("name", routine.name)
            put("sort_order", routine.sortOrder)
            put("created_at", SEED_CREATED_AT)
        }
        db.insert("routine", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    /** La versión inicial de cada rutina comparte su identificador con la rutina. */
    private fun insertRoutineVersion(db: SupportSQLiteDatabase, routineId: Long) {
        val values = ContentValues().apply {
            put("id", routineId)
            put("routine_id", routineId)
            put("version_number", FIRST_VERSION)
        }
        db.insert("routine_version", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun insertRoutineCurrentVersion(db: SupportSQLiteDatabase, routineId: Long) {
        val values = ContentValues().apply {
            put("routine_id", routineId)
            put("current_version_number", FIRST_VERSION)
        }
        db.insert("routine_current_version", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun insertPlanAssignment(db: SupportSQLiteDatabase, assignment: SeedAssignment) {
        val values = ContentValues().apply {
            put("routine_version_id", assignment.routineVersionId)
            put("exercise_id", assignment.exerciseId)
            put("sets", assignment.sets)
            put("reps", assignment.reps)
            put("sort_order", assignment.sortOrder)
            put("slot", assignment.slot)
        }
        db.insert("plan_assignment", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
