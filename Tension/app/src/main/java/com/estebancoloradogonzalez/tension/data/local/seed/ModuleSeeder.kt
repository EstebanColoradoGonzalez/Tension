package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object ModuleSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        seedModules(db)
        seedMuscleZones(db)
        seedEquipmentTypes(db)
    }

    private fun seedModules(db: SupportSQLiteDatabase) {
        insertModule(db, "A", "Módulo A \u2014 Superior (Pull + Abs)", "Espalda, Bíceps, Abdomen", 2.5)
        insertModule(db, "B", "Módulo B \u2014 Superior (Push)", "Pecho, Hombro, Tríceps", 2.5)
        insertModule(db, "C", "Módulo C \u2014 Inferior", "Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos", 5.0)
    }

    private fun insertModule(db: SupportSQLiteDatabase, code: String, name: String, groupDescription: String, loadIncrementKg: Double) {
        val values = ContentValues().apply {
            put("code", code)
            put("name", name)
            put("group_description", groupDescription)
            put("load_increment_kg", loadIncrementKg)
        }
        db.insert("module", SQLiteDatabase.CONFLICT_REPLACE, values)
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
    }

    private fun insertMuscleZone(db: SupportSQLiteDatabase, id: Long, name: String, muscleGroup: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("muscle_group", muscleGroup)
        }
        db.insert("muscle_zone", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun seedEquipmentTypes(db: SupportSQLiteDatabase) {
        insertEquipmentType(db, 1, "Máquina")
        insertEquipmentType(db, 2, "Mancuernas")
        insertEquipmentType(db, 3, "Barra de Pesas")
        insertEquipmentType(db, 4, "Cuerpo")
        insertEquipmentType(db, 5, "Mancuerna")
        insertEquipmentType(db, 6, "Polea")
        insertEquipmentType(db, 7, "Pesa")
        insertEquipmentType(db, 8, "Mancuerna o Pesa Rusa")
        insertEquipmentType(db, 9, "Máquina Multiestación")
    }

    private fun insertEquipmentType(db: SupportSQLiteDatabase, id: Long, name: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
        }
        db.insert("equipment_type", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
