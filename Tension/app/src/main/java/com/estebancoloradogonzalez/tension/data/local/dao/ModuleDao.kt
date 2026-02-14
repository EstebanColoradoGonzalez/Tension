package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {

    @Query("SELECT * FROM module ORDER BY code ASC")
    fun getAll(): Flow<List<ModuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<ModuleEntity>)
}
