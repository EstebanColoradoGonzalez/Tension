package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseMuscleZoneEntity
import kotlinx.coroutines.flow.Flow

data class ExerciseWithDetails(
    val id: Long,
    val name: String,
    val moduleCode: String,
    val moduleName: String,
    val equipmentTypeName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val isCustom: Int,
    val mediaResource: String?,
    val muscleZones: String?,
)

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT 
            e.id,
            e.name,
            e.module_code AS moduleCode,
            m.name AS moduleName,
            et.name AS equipmentTypeName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones
        FROM exercise e
        INNER JOIN module m ON e.module_code = m.code
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        GROUP BY e.id
        ORDER BY e.module_code ASC, e.name ASC
        """,
    )
    fun getAll(): Flow<List<ExerciseWithDetails>>

    @Query(
        """
        SELECT 
            e.id,
            e.name,
            e.module_code AS moduleCode,
            m.name AS moduleName,
            et.name AS equipmentTypeName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones
        FROM exercise e
        INNER JOIN module m ON e.module_code = m.code
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE e.id = :exerciseId
        GROUP BY e.id
        """,
    )
    fun getById(exerciseId: Long): Flow<ExerciseWithDetails?>

    @Query(
        """
        SELECT 
            e.id,
            e.name,
            e.module_code AS moduleCode,
            m.name AS moduleName,
            et.name AS equipmentTypeName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones
        FROM exercise e
        INNER JOIN module m ON e.module_code = m.code
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE e.module_code = :moduleCode
          AND e.id NOT IN (
              SELECT exercise_id FROM plan_assignment WHERE module_version_id = :moduleVersionId
          )
        GROUP BY e.id
        ORDER BY e.name ASC
        """,
    )
    fun getByModuleCodeNotInVersion(
        moduleCode: String,
        moduleVersionId: Long,
    ): Flow<List<ExerciseWithDetails>>

    @Query(
        """
        SELECT 
            e.id,
            e.name,
            e.module_code AS moduleCode,
            m.name AS moduleName,
            et.name AS equipmentTypeName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones
        FROM exercise e
        INNER JOIN module m ON e.module_code = m.code
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE e.module_code = :moduleCode
          AND e.id NOT IN (:excludedExerciseIds)
        GROUP BY e.id
        ORDER BY e.name ASC
        """,
    )
    fun getByModuleCodeNotInIds(
        moduleCode: String,
        excludedExerciseIds: List<Long>,
    ): Flow<List<ExerciseWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscleZone(zone: ExerciseMuscleZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMuscleZones(zones: List<ExerciseMuscleZoneEntity>)

    @Query("UPDATE exercise SET media_resource = :mediaResource WHERE id = :exerciseId")
    suspend fun updateMediaResource(exerciseId: Long, mediaResource: String?)

    @Query("SELECT COUNT(*) FROM exercise WHERE name = :name AND equipment_type_id = :equipmentTypeId")
    suspend fun countByNameAndEquipment(name: String, equipmentTypeId: Long): Int

    @Transaction
    suspend fun insertExerciseWithMuscleZones(
        exercise: ExerciseEntity,
        muscleZones: List<ExerciseMuscleZoneEntity>,
    ): Long {
        val exerciseId = insert(exercise)
        val zonesWithId = muscleZones.map { it.copy(exerciseId = exerciseId) }
        insertAllMuscleZones(zonesWithId)
        return exerciseId
    }
}
