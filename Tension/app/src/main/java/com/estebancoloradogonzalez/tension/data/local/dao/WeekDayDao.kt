package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.WeekDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDayDao {

    @Query("SELECT * FROM week_day ORDER BY id ASC")
    fun getAll(): Flow<List<WeekDayEntity>>

    @Query("SELECT * FROM week_day ORDER BY id ASC")
    suspend fun getAllOnce(): List<WeekDayEntity>

    @Query("SELECT * FROM week_day WHERE id = :id")
    fun getByIdFlow(id: Int): Flow<WeekDayEntity?>

    @Query("SELECT * FROM week_day WHERE id = :id")
    suspend fun getByIdOnce(id: Int): WeekDayEntity?

    @Update
    suspend fun update(weekDay: WeekDayEntity)
}
