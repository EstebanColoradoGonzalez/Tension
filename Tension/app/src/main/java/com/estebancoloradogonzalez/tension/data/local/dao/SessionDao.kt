package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

data class ActiveSessionInfo(
    val sessionId: Long,
    val moduleCode: String,
    val versionNumber: Int,
    val totalExercises: Int,
    val completedExercises: Int,
)

data class SessionSummaryInfo(
    val status: String,
    val moduleCode: String,
    val versionNumber: Int,
    val totalTonnageKg: Double,
    val totalExercises: Int,
    val completedExercises: Int,
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM session WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE id = :sessionId")
    fun getById(sessionId: Long): Flow<SessionEntity?>

    @Query(
        """
        SELECT 
            s.id AS sessionId,
            mv.module_code AS moduleCode,
            mv.version_number AS versionNumber,
            (SELECT COUNT(*) FROM session_exercise WHERE session_id = s.id) AS totalExercises,
            (SELECT COUNT(*) FROM session_exercise se2
             INNER JOIN (
                 SELECT session_exercise_id, COUNT(*) AS cnt
                 FROM exercise_set GROUP BY session_exercise_id HAVING cnt >= 4
             ) completed ON se2.id = completed.session_exercise_id
             WHERE se2.session_id = s.id
            ) AS completedExercises
        FROM session s
        INNER JOIN module_version mv ON s.module_version_id = mv.id
        WHERE s.status = 'IN_PROGRESS'
        LIMIT 1
        """,
    )
    fun getActiveSessionWithModuleVersion(): Flow<ActiveSessionInfo?>

    @Query("UPDATE session SET status = :status WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Long, status: String)

    @Query("SELECT module_version_id FROM session WHERE id = :sessionId")
    suspend fun getModuleVersionIdBySessionId(sessionId: Long): Long

    @Query("SELECT deload_id FROM session WHERE id = :sessionId")
    suspend fun getDeloadIdBySessionId(sessionId: Long): Long?

    @Query(
        """
        SELECT
            s.status,
            mv.module_code AS moduleCode,
            mv.version_number AS versionNumber,
            COALESCE(
                (SELECT SUM(es.weight_kg * es.reps)
                 FROM exercise_set es
                 INNER JOIN session_exercise se ON es.session_exercise_id = se.id
                 WHERE se.session_id = s.id),
                0.0
            ) AS totalTonnageKg,
            (SELECT COUNT(*) FROM session_exercise WHERE session_id = s.id) AS totalExercises,
            (SELECT COUNT(*) FROM session_exercise se2
             INNER JOIN (
                 SELECT session_exercise_id, COUNT(*) AS cnt
                 FROM exercise_set GROUP BY session_exercise_id HAVING cnt >= 4
             ) completed ON se2.id = completed.session_exercise_id
             WHERE se2.session_id = s.id
            ) AS completedExercises
        FROM session s
        INNER JOIN module_version mv ON s.module_version_id = mv.id
        WHERE s.id = :sessionId
        """,
    )
    suspend fun getSessionSummaryInfo(sessionId: Long): SessionSummaryInfo
}
