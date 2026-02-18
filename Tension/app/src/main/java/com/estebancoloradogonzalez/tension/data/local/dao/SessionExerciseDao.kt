package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import kotlinx.coroutines.flow.Flow

data class SessionExerciseWithDetails(
    val sessionExerciseId: Long,
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
    val completedSets: Int,
    val loadIncrementKg: Double,
)

data class SetExerciseInfo(
    val exerciseId: Long,
    val exerciseName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val totalSets: Int,
)

data class SessionExerciseForSubstitution(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val originalExerciseId: Long?,
    val exerciseName: String,
    val moduleCode: String,
    val completedSets: Int,
)

data class SessionExerciseForProgression(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val isBodyweight: Int,
    val isIsometric: Int,
    val moduleCode: String,
    val loadIncrementKg: Double,
)

data class ExerciseSummaryDto(
    val exerciseId: Long,
    val exerciseName: String,
    val classification: String?,
    val isBodyweight: Int,
    val isIsometric: Int,
    val prescribedLoadKg: Double?,
    val avgWeightKg: Double,
    val totalReps: Int,
    val setCount: Int,
    val isMastered: Int,
    val moduleCode: String,
    val previousTotalReps: Int?,
)

@Dao
interface SessionExerciseDao {

    @Insert
    suspend fun insertAll(exercises: List<SessionExerciseEntity>)

    @Query("SELECT * FROM session_exercise WHERE session_id = :sessionId")
    fun getBySessionId(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query(
        """
        SELECT 
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            et.name AS equipmentTypeName,
            GROUP_CONCAT(DISTINCT mz.name) AS muscleZones,
            COALESCE(pa.sets, 4) AS sets,
            COALESCE(pa.reps, '8-12') AS reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            ep.prescribed_load_kg AS prescribedLoadKg,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets,
            m.load_increment_kg AS loadIncrementKg
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN module m ON e.module_code = m.code
        LEFT JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
            AND pa.exercise_id = se.exercise_id
        LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
        LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        LEFT JOIN exercise_progression ep ON e.id = ep.exercise_id
        WHERE se.session_id = :sessionId
        GROUP BY se.id
        ORDER BY e.name ASC
        """,
    )
    fun getBySessionIdWithDetails(sessionId: Long): Flow<List<SessionExerciseWithDetails>>

    @Query(
        """
        SELECT
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            COALESCE(pa.sets, 4) AS totalSets
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN session s ON se.session_id = s.id
        LEFT JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
            AND pa.exercise_id = se.exercise_id
        WHERE se.id = :sessionExerciseId
        """,
    )
    suspend fun getExerciseInfoForSet(sessionExerciseId: Long): SetExerciseInfo?

    @Query(
        """
        SELECT
            se.id,
            se.session_id AS sessionId,
            se.exercise_id AS exerciseId,
            se.original_exercise_id AS originalExerciseId,
            e.name AS exerciseName,
            e.module_code AS moduleCode,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        WHERE se.id = :sessionExerciseId
        """,
    )
    suspend fun getSessionExerciseForSubstitution(sessionExerciseId: Long): SessionExerciseForSubstitution?

    @Query("SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId")
    suspend fun getExerciseIdsForSession(sessionId: Long): List<Long>

    @Query(
        """
        SELECT 
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.module_code AS moduleCode,
            m.load_increment_kg AS loadIncrementKg
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN module m ON e.module_code = m.code
        WHERE se.session_id = :sessionId
        """,
    )
    suspend fun getSessionExercisesForProgression(sessionId: Long): List<SessionExerciseForProgression>

    @Query(
        """
        UPDATE session_exercise
        SET progression_classification = :classification
        WHERE id = :sessionExerciseId
        """,
    )
    suspend fun updateProgressionClassification(sessionExerciseId: Long, classification: String?)

    @Query(
        """
        UPDATE session_exercise
        SET exercise_id = :newExerciseId,
            original_exercise_id = :originalExerciseId
        WHERE id = :sessionExerciseId
        """,
    )
    suspend fun updateExerciseId(sessionExerciseId: Long, newExerciseId: Long, originalExerciseId: Long)

    @Query(
        """
        SELECT
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            se.progression_classification AS classification,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            ep.prescribed_load_kg AS prescribedLoadKg,
            COALESCE(
                (SELECT AVG(es.weight_kg) FROM exercise_set es WHERE es.session_exercise_id = se.id),
                0.0
            ) AS avgWeightKg,
            COALESCE(
                (SELECT SUM(es.reps) FROM exercise_set es WHERE es.session_exercise_id = se.id),
                0
            ) AS totalReps,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS setCount,
            CASE WHEN ep.status = 'MASTERED' THEN 1 ELSE 0 END AS isMastered,
            e.module_code AS moduleCode,
            (SELECT SUM(es3.reps)
             FROM exercise_set es3
             WHERE es3.session_exercise_id = (
                 SELECT se3.id
                 FROM session_exercise se3
                 INNER JOIN session s3 ON se3.session_id = s3.id
                 WHERE se3.exercise_id = se.exercise_id
                   AND s3.id != :sessionId
                   AND s3.status IN ('COMPLETED', 'INCOMPLETE')
                 ORDER BY s3.date DESC, s3.id DESC
                 LIMIT 1
             )
            ) AS previousTotalReps
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        LEFT JOIN exercise_progression ep ON e.id = ep.exercise_id
        WHERE se.session_id = :sessionId
        GROUP BY se.id
        HAVING setCount > 0
        ORDER BY e.name ASC
        """,
    )
    suspend fun getExercisesForSummary(sessionId: Long): List<ExerciseSummaryDto>
}
