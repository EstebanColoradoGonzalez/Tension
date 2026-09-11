package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.MuscleZoneEntity
import kotlinx.coroutines.flow.Flow

/**
 * El orden es siempre el del catálogo (`sort_order`), no el alfabético: los 14 grupos
 * musculares en orden anatómico y, dentro de cada uno, las zonas de mayor a menor. Ordenar
 * por nombre daría *Pectoral Inferior, Mayor, Medio, Superior*, que no significa nada.
 */
@Dao
interface MuscleZoneDao {

    @Query("SELECT * FROM muscle_zone ORDER BY sort_order ASC")
    fun getAll(): Flow<List<MuscleZoneEntity>>

    @Query(
        """
        SELECT DISTINCT mz.* FROM muscle_zone mz
        INNER JOIN exercise_muscle_zone emz ON mz.id = emz.muscle_zone_id
        ORDER BY mz.sort_order ASC
        """,
    )
    fun getWithExercises(): Flow<List<MuscleZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zones: List<MuscleZoneEntity>)
}
