package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedExercise

/**
 * Inserta el catálogo base de ejercicios, sus zonas musculares y sus opciones de
 * equipamiento.
 *
 * Los datos residen en [ExerciseCatalog]; aquí solo se mapean a `ContentValues`.
 */
object ExerciseSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        ExerciseCatalog.ALL.forEach { exercise ->
            insertExercise(db, exercise)
            exercise.muscleZoneIds.forEach { muscleZoneId ->
                insertExerciseMuscleZone(db, exercise.id, muscleZoneId)
            }
            exercise.equipmentTypeIds.forEach { equipmentTypeId ->
                insertExerciseEquipment(db, exercise.id, equipmentTypeId)
            }
        }
    }

    private fun insertExercise(db: SupportSQLiteDatabase, exercise: SeedExercise) {
        val values = ContentValues().apply {
            put("id", exercise.id)
            put("name", exercise.name)
            put("is_bodyweight", exercise.isBodyweight.toFlag())
            put("is_isometric", exercise.isIsometric.toFlag())
            put("is_to_technical_failure", exercise.isToTechnicalFailure.toFlag())
            put("is_custom", 0)
            put("media_resource", exercise.mediaResource)
            put("progression_difficulty", exercise.progressionDifficulty.name)
        }
        db.insert("exercise", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun insertExerciseMuscleZone(db: SupportSQLiteDatabase, exerciseId: Long, muscleZoneId: Long) {
        val values = ContentValues().apply {
            put("exercise_id", exerciseId)
            put("muscle_zone_id", muscleZoneId)
        }
        db.insert("exercise_muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun insertExerciseEquipment(db: SupportSQLiteDatabase, exerciseId: Long, equipmentTypeId: Long) {
        val values = ContentValues().apply {
            put("exercise_id", exerciseId)
            put("equipment_type_id", equipmentTypeId)
        }
        db.insert("exercise_equipment", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun Boolean.toFlag(): Int = if (this) 1 else 0
}
