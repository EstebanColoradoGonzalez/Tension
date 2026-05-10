package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineCurrentVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineCurrentVersionDao {

    @Query("SELECT * FROM routine_current_version")
    fun getAll(): Flow<List<RoutineCurrentVersionEntity>>

    @Query("SELECT * FROM routine_current_version")
    suspend fun getAllOnce(): List<RoutineCurrentVersionEntity>

    @Query("SELECT * FROM routine_current_version WHERE routine_id = :routineId")
    suspend fun getByRoutineId(routineId: Long): RoutineCurrentVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineCurrentVersionEntity)

    @Update
    suspend fun update(entity: RoutineCurrentVersionEntity)

    @Query("DELETE FROM routine_current_version WHERE routine_id = :routineId")
    suspend fun deleteByRoutineId(routineId: Long)
}
