package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ModuleVersionEntity
import kotlinx.coroutines.flow.Flow

data class ModuleVersionWithCount(
    val id: Long,
    val moduleCode: String,
    val versionNumber: Int,
    val exerciseCount: Int,
)

@Dao
interface ModuleVersionDao {

    @Query("SELECT * FROM module_version ORDER BY module_code ASC, version_number ASC")
    fun getAll(): Flow<List<ModuleVersionEntity>>

    @Query(
        """
        SELECT 
            mv.id,
            mv.module_code AS moduleCode,
            mv.version_number AS versionNumber,
            COUNT(pa.exercise_id) AS exerciseCount
        FROM module_version mv
        LEFT JOIN plan_assignment pa ON mv.id = pa.module_version_id
        GROUP BY mv.id
        ORDER BY mv.module_code ASC, mv.version_number ASC
        """,
    )
    fun getAllWithExerciseCount(): Flow<List<ModuleVersionWithCount>>

    @Query("SELECT * FROM module_version WHERE id = :moduleVersionId")
    fun getById(moduleVersionId: Long): Flow<ModuleVersionEntity?>

    @Query("SELECT * FROM module_version WHERE module_code = :moduleCode AND version_number = :versionNumber LIMIT 1")
    fun getByModuleCodeAndVersion(moduleCode: String, versionNumber: Int): Flow<ModuleVersionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(versions: List<ModuleVersionEntity>)
}
