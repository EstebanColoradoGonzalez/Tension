package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object BaseDataSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        seedMuscleZones(db)
        seedEquipmentTypes(db)
    }

    /** Los datos residen en [MuscleZoneCatalog]; aquí solo se mapean a `ContentValues`. */
    private fun seedMuscleZones(db: SupportSQLiteDatabase) {
        MuscleZoneCatalog.ALL.forEach { zone ->
            insertMuscleZone(db, zone.id, zone.name, zone.muscleGroup, zone.sortOrder)
        }
    }

    private fun insertMuscleZone(
        db: SupportSQLiteDatabase,
        id: Long,
        name: String,
        muscleGroup: String,
        sortOrder: Int,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("muscle_group", muscleGroup)
            put("sort_order", sortOrder)
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
