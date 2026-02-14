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
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            ep.prescribed_load_kg AS prescribedLoadKg,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN equipment_type et ON e.equipment_type_id = et.id
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
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
}
