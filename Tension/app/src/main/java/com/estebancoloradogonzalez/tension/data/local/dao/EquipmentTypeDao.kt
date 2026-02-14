package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.EquipmentTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentTypeDao {

    @Query("SELECT * FROM equipment_type ORDER BY name ASC")
    fun getAll(): Flow<List<EquipmentTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<EquipmentTypeEntity>)
}
