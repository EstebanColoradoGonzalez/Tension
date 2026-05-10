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

    @Query(
        """
        SELECT DISTINCT et.* FROM equipment_type et
        INNER JOIN exercise e ON et.id = e.equipment_type_id
        ORDER BY et.name ASC
        """,
    )
    fun getWithExercises(): Flow<List<EquipmentTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<EquipmentTypeEntity>)
}
