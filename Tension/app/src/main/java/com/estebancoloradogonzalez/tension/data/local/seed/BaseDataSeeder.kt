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
        insertEquipmentType(db, 10, "Mancuernas o Polea")
        insertEquipmentType(db, 11, "Polea con Cuerda")
        insertEquipmentType(db, 12, "Polea con Cuerda o Polea con Barra en V")
        insertEquipmentType(db, 13, "Barra")
        insertEquipmentType(db, 14, "Mancuerna o Polea o Barra")
        insertEquipmentType(db, 15, "Barra o Mancuernas")
        insertEquipmentType(db, 16, "Banda Elástica")
        insertEquipmentType(db, 17, "Kettlebell")
        insertEquipmentType(db, 18, "Barra EZ")
        insertEquipmentType(db, 19, "TRX/Suspensión")
        insertEquipmentType(db, 20, "Balón Medicinal")
        insertEquipmentType(db, 21, "Rodillo de Abdomen")
        insertEquipmentType(db, 22, "Paralelas/Dip Station")
        insertEquipmentType(db, 23, "Barra Fija")
    }

    private fun insertEquipmentType(db: SupportSQLiteDatabase, id: Long, name: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
        }
        db.insert("equipment_type", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
