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
    val equipmentTypeName: String,
    val muscleZones: String?,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val isCustom: Int,
    val slot: Int,
)

data class SessionPreviewExerciseDto(
    val exerciseId: Long,
    val exerciseName: String,
    val equipmentTypeName: String,
    val muscleZones: String?,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val prescribedLoadKg: Double?,
    val muscleGroup: String?,
    val slot: Int,
)

@Dao
interface PlanAssignmentDao {

    @Query("SELECT * FROM plan_assignment WHERE routine_version_id = :routineVersionId ORDER BY sort_order ASC")
    fun getByRoutineVersionId(routineVersionId: Long): Flow<List<PlanAssignmentEntity>>

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            et.name AS equipmentTypeName,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE pa.routine_version_id = :routineVersionId
        GROUP BY e.id
        ORDER BY pa.sort_order ASC
        """,
    )
    fun getDetailsByRoutineVersionId(routineVersionId: Long): Flow<List<PlanAssignmentWithExerciseDetails>>

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            et.name AS equipmentTypeName,
            GROUP_CONCAT(DISTINCT mz.name) AS muscleZones,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            ep.prescribed_load_kg AS prescribedLoadKg,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = e.id LIMIT 1) AS muscleGroup,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        LEFT JOIN exercise_progression ep ON e.id = ep.exercise_id
        WHERE pa.routine_version_id = :routineVersionId
        GROUP BY e.id
        ORDER BY pa.sort_order ASC
        """,
    )
    fun getPreviewByRoutineVersionId(routineVersionId: Long): Flow<List<SessionPreviewExerciseDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<PlanAssignmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assignment: PlanAssignmentEntity)

    @Query("DELETE FROM plan_assignment WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId")
    suspend fun delete(routineVersionId: Long, exerciseId: Long)

    @Query("DELETE FROM plan_assignment WHERE routine_version_id = :routineVersionId AND slot = :slot")
    suspend fun deleteBySlot(routineVersionId: Long, slot: Int)

    @Query("SELECT MAX(sort_order) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getMaxSortOrder(routineVersionId: Long): Int?

    @Query("SELECT MAX(slot) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getMaxSlot(routineVersionId: Long): Int?

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            et.name AS equipmentTypeName,
            GROUP_CONCAT(mz.name, ', ') AS muscleZones,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE pa.routine_version_id = :routineVersionId AND pa.slot = :slot
        GROUP BY e.id
        ORDER BY e.name ASC
        """,
    )
    suspend fun getAlternativesForSlot(routineVersionId: Long, slot: Int): List<PlanAssignmentWithExerciseDetails>

    @Query("SELECT COUNT(*) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun countExercisesForRoutineVersion(routineVersionId: Long): Int

    /** Counts the number of distinct slots in a routine version (one slot = one exercise position). */
    @Query("SELECT COUNT(DISTINCT slot) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun countDistinctSlots(routineVersionId: Long): Int

    @Query("SELECT exercise_id FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getExerciseIdsByRoutineVersionId(routineVersionId: Long): List<Long>

    /**
     * Counts the number of distinct SLOTS that have at least one exercise either IN_PLATEAU
     * or with a REGRESSION classification in this session. Uses slots as the unit so that
     * the presence of alternatives does not dilute the affected ratio.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT pa.slot)
        FROM plan_assignment pa
        LEFT JOIN exercise_progression ep ON pa.exercise_id = ep.exercise_id
        LEFT JOIN session_exercise se ON (
            se.session_id = :sessionId
            AND (se.exercise_id = pa.exercise_id OR se.original_exercise_id = pa.exercise_id)
        )
        WHERE pa.routine_version_id = :routineVersionId
        AND (ep.status = 'IN_PLATEAU' OR se.progression_classification = 'REGRESSION')
        """,
    )
    suspend fun countAffectedSlotsForDeload(routineVersionId: Long, sessionId: Long): Int

    @Query(
        """
        SELECT DISTINCT mz.name
        FROM plan_assignment pa
        INNER JOIN exercise_muscle_zone emz ON pa.exercise_id = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        INNER JOIN routine_version rv ON pa.routine_version_id = rv.id
        WHERE rv.routine_id = :routineId
        ORDER BY mz.name ASC
        """,
    )
    suspend fun getMuscleZoneNamesByRoutineId(routineId: Long): List<String>

    @Query(
        """
        UPDATE plan_assignment
        SET sets = :sets, reps = :reps
        WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId
        """,
    )
    suspend fun updateSetsAndReps(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)

    @Query(
        """
        UPDATE plan_assignment
        SET sets = :sets, reps = :reps
        WHERE routine_version_id = :routineVersionId AND slot = :slot
        """,
    )
    suspend fun updateSetsAndRepsBySlot(routineVersionId: Long, slot: Int, sets: Int, reps: String)

    @Query("SELECT slot FROM plan_assignment WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId")
    suspend fun getSlotForExercise(routineVersionId: Long, exerciseId: Long): Int?
}
