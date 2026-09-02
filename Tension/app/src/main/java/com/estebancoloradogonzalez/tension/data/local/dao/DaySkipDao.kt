package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.DaySkipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DaySkipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(daySkip: DaySkipEntity)

    @Query("SELECT * FROM day_skip WHERE id = 1")
    fun getSkip(): Flow<DaySkipEntity?>

    @Query("SELECT * FROM day_skip WHERE id = 1")
    suspend fun getSkipOnce(): DaySkipEntity?

    @Query("DELETE FROM day_skip")
    suspend fun clear()
}
