package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.PlanAssignmentEntity
import kotlinx.coroutines.flow.Flow

data class PlanAssignmentWithExerciseDetails(
    val exerciseId: Long,
    val exerciseName: String,
    val moduleCode: String,
    val equipmentTypeName: String,
    val muscleZones: String,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val isCustom: Int,
)

@Dao
interface PlanAssignmentDao {

    @Query("SELECT * FROM plan_assignment WHERE module_version_id = :moduleVersionId")
    fun getByModuleVersionId(moduleVersionId: Long): Flow<List<PlanAssignmentEntity>>

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            e.module_code AS moduleCode,
            et.name AS equipmentTypeName,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE pa.module_version_id = :moduleVersionId
        GROUP BY e.id
        ORDER BY e.name ASC
        """,
    )
    fun getDetailsByModuleVersionId(moduleVersionId: Long): Flow<List<PlanAssignmentWithExerciseDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<PlanAssignmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assignment: PlanAssignmentEntity)

    @Query("DELETE FROM plan_assignment WHERE module_version_id = :moduleVersionId AND exercise_id = :exerciseId")
    suspend fun delete(moduleVersionId: Long, exerciseId: Long)

    @Query("SELECT COUNT(*) FROM plan_assignment WHERE module_version_id = :moduleVersionId")
    suspend fun countExercisesForModuleVersion(moduleVersionId: Long): Int

    @Query(
        """
        SELECT COUNT(DISTINCT pa.exercise_id)
        FROM plan_assignment pa
        LEFT JOIN exercise_progression ep ON pa.exercise_id = ep.exercise_id
        LEFT JOIN session_exercise se ON pa.exercise_id = se.exercise_id
            AND se.session_id = :sessionId
        WHERE pa.module_version_id = :moduleVersionId
        AND (ep.status = 'IN_PLATEAU' OR se.progression_classification = 'REGRESSION')
        """,
    )
    suspend fun countAffectedForDeload(moduleVersionId: Long, sessionId: Long): Int
}
