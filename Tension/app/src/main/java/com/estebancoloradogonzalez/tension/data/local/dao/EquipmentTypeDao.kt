package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.EquipmentTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentTypeDao {

    /**
     * Ordenado por `id`, no por nombre.
     *
     * El identificador encoda el orden declarado del catálogo (ver `EquipmentCatalog`), que
     * es el que presenta la lista de casillas del formulario de ejercicio. La tabla es
     * cerrada y sembrada —no hay interfaz para crear tipos de equipamiento— así que su id
     * es su posición y no un accidente de inserción.
     */
    @Query("SELECT * FROM equipment_type ORDER BY id ASC")
    fun getAll(): Flow<List<EquipmentTypeEntity>>

    /** Solo los tipos con al menos un ejercicio: filtrar por algo sin resultados es ruido. */
    @Query(
        """
        SELECT DISTINCT et.* FROM equipment_type et
        INNER JOIN exercise_equipment ee ON et.id = ee.equipment_type_id
        ORDER BY et.id ASC
        """,
    )
    fun getWithExercises(): Flow<List<EquipmentTypeEntity>>

    @Query("SELECT * FROM equipment_type WHERE id IN (:ids) ORDER BY id ASC")
    fun getByIds(ids: List<Long>): Flow<List<EquipmentTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<EquipmentTypeEntity>)
}
