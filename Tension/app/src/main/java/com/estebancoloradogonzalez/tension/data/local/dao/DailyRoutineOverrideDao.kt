package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.DailyRoutineOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRoutineOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: DailyRoutineOverrideEntity)

    @Query("SELECT * FROM daily_routine_override WHERE id = 1")
    fun getOverride(): Flow<DailyRoutineOverrideEntity?>

    @Query("SELECT * FROM daily_routine_override WHERE id = 1")
    suspend fun getOverrideOnce(): DailyRoutineOverrideEntity?

    @Query("DELETE FROM daily_routine_override")
    suspend fun clear()
}
