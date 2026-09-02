package com.estebancoloradogonzalez.tension.data.local.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedWeekDay

/**
 * Inserta los 7 días de la semana y su relación con la rutina que les corresponde.
 *
 * Los datos residen en [DefaultWeekDays]; aquí solo se mapean a `ContentValues`. Debe
 * ejecutarse **después** de [PlanSeeder]: la clave foránea a `routine` exige que las
 * rutinas ya existan.
 */
object WeekDaySeeder {

    fun seed(db: SupportSQLiteDatabase) {
        DefaultWeekDays.ALL.forEach { weekDay -> insertWeekDay(db, weekDay) }
    }

    private fun insertWeekDay(db: SupportSQLiteDatabase, weekDay: SeedWeekDay) {
        val values = ContentValues().apply {
            put("id", weekDay.weekDay.isoNumber)
            put("code", weekDay.weekDay.code)
            if (weekDay.routineId == null) {
                putNull("routine_id")
            } else {
                put("routine_id", weekDay.routineId)
            }
        }
        db.insert("week_day", SQLiteDatabase.CONFLICT_REPLACE, values)
    }
}
