package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineVersionEntity
import kotlinx.coroutines.flow.Flow

data class RoutineVersionWithCount(
    val id: Long,
    val routineId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)

@Dao
interface RoutineVersionDao {

    @Query("SELECT * FROM routine_version WHERE routine_id = :routineId ORDER BY version_number ASC")
    fun getByRoutineId(routineId: Long): Flow<List<RoutineVersionEntity>>

    @Query("SELECT * FROM routine_version WHERE id = :id")
    fun getById(id: Long): Flow<RoutineVersionEntity?>

    @Query("SELECT * FROM routine_version WHERE routine_id = :routineId AND version_number = :versionNumber")
    suspend fun getByRoutineIdAndVersion(routineId: Long, versionNumber: Int): RoutineVersionEntity?

    @Insert
    suspend fun insert(version: RoutineVersionEntity): Long

    @Query("DELETE FROM routine_version WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM routine_version WHERE routine_id = :routineId")
    suspend fun countByRoutineId(routineId: Long): Int

    @Query("SELECT MAX(version_number) FROM routine_version WHERE routine_id = :routineId")
    suspend fun getMaxVersionNumber(routineId: Long): Int?

    @Query("SELECT MIN(version_number) FROM routine_version WHERE routine_id = :routineId")
    suspend fun getMinVersionNumber(routineId: Long): Int?

    @Query("SELECT version_number FROM routine_version WHERE routine_id = :routineId")
    suspend fun getVersionNumbersByRoutineId(routineId: Long): List<Int>

    @Query(
        """
        SELECT 
            rv.id,
            rv.routine_id AS routineId,
            rv.version_number AS versionNumber,
            COUNT(pa.exercise_id) AS exerciseCount
        FROM routine_version rv
        LEFT JOIN plan_assignment pa ON rv.id = pa.routine_version_id
        GROUP BY rv.id
        ORDER BY rv.routine_id ASC, rv.version_number ASC
        """,
    )
    fun getAllWithExerciseCount(): Flow<List<RoutineVersionWithCount>>

    @Query(
        """
        SELECT 
            rv.id,
            rv.routine_id AS routineId,
            rv.version_number AS versionNumber,
            COUNT(pa.exercise_id) AS exerciseCount
        FROM routine_version rv
        LEFT JOIN plan_assignment pa ON rv.id = pa.routine_version_id
        WHERE rv.routine_id = :routineId
        GROUP BY rv.id
        ORDER BY rv.version_number ASC
        """,
    )
    fun getByRoutineIdWithExerciseCount(routineId: Long): Flow<List<RoutineVersionWithCount>>
}
