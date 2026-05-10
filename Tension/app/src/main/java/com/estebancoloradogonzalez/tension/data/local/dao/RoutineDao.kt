package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routine ORDER BY sort_order ASC")
    fun getAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine ORDER BY sort_order ASC")
    suspend fun getAllOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT MAX(sort_order) FROM routine")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM routine")
    suspend fun countRoutines(): Int

    @Query("UPDATE routine SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM routine WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM routine WHERE name = :name AND id != :excludeId)")
    suspend fun existsByNameExcluding(name: String, excludeId: Long): Boolean
}
