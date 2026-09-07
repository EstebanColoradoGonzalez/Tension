package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object BaseDataSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        seedMuscleZones(db)
        seedEquipmentTypes(db)
    }

    private fun seedMuscleZones(db: SupportSQLiteDatabase) {
        insertMuscleZone(db, 1, "Pecho Medio", "Pecho")
        insertMuscleZone(db, 2, "Pecho Superior", "Pecho")
        insertMuscleZone(db, 3, "Pecho Inferior", "Pecho")
        insertMuscleZone(db, 4, "Espalda Media", "Espalda")
        insertMuscleZone(db, 5, "Dorsal Ancho", "Espalda")
        insertMuscleZone(db, 6, "Abdomen", "Abdomen")
        insertMuscleZone(db, 7, "Hombro", "Hombro")
        insertMuscleZone(db, 8, "Tríceps", "Tríceps")
        insertMuscleZone(db, 9, "Bíceps", "Bíceps")
        insertMuscleZone(db, 10, "Cuádriceps", "Cuádriceps")
        insertMuscleZone(db, 11, "Isquiotibiales", "Isquiotibiales")
        insertMuscleZone(db, 12, "Aductores", "Aductores")
        insertMuscleZone(db, 13, "Abductores", "Abductores")
        insertMuscleZone(db, 14, "Gemelos", "Gemelos")
        insertMuscleZone(db, 15, "Glúteos", "Glúteos")
        insertMuscleZone(db, 16, "Espalda Alta", "Espalda")
        insertMuscleZone(db, 17, "Trapecio", "Espalda")
        insertMuscleZone(db, 18, "Espalda Baja", "Espalda")
        insertMuscleZone(db, 19, "Antebrazo", "Antebrazo")
        insertMuscleZone(db, 20, "Cuello", "Cuello")
    }

    private fun insertMuscleZone(db: SupportSQLiteDatabase, id: Long, name: String, muscleGroup: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("muscle_group", muscleGroup)
        }
        db.insert("muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    /** Los datos residen en [EquipmentCatalog]; aquí solo se mapean a `ContentValues`. */
    private fun seedEquipmentTypes(db: SupportSQLiteDatabase) {
        EquipmentCatalog.ALL.forEach { type ->
            insertEquipmentType(db, type.id, type.name)
        }
    }

    private fun insertEquipmentType(db: SupportSQLiteDatabase, id: Long, name: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
        }
        db.insert("equipment_type", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
