package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

data class ActiveSessionInfo(
    val sessionId: Long,
    val routineName: String,
    val versionNumber: Int,
    val totalExercises: Int,
    val completedExercises: Int,
)

data class SessionSummaryInfo(
    val status: String,
    val routineName: String,
    val versionNumber: Int,
    val routineId: Long,
    val totalTonnageKg: Double,
    val totalExercises: Int,
    val completedExercises: Int,
)

data class ClosedSessionDto(
    val sessionId: Long,
    val date: String,
    val routineName: String,
    val versionNumber: Int,
    val status: String,
    val totalTonnageKg: Double,
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
            r.name AS routineName,
            rv.version_number AS versionNumber,
            (SELECT COUNT(*) FROM session_exercise WHERE session_id = s.id AND pending_selection = 0) AS totalExercises,
            (SELECT COUNT(*) FROM session_exercise se2
             WHERE se2.session_id = s.id
               AND se2.is_finalized = 1
            ) AS completedExercises
        FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE s.status = 'IN_PROGRESS'
        LIMIT 1
        """,
    )
    fun getActiveSessionWithRoutineVersion(): Flow<ActiveSessionInfo?>

    @Query("UPDATE session SET status = :status WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Long, status: String)

    @Query("SELECT routine_version_id FROM session WHERE id = :sessionId")
    suspend fun getRoutineVersionIdBySessionId(sessionId: Long): Long

    @Query("SELECT deload_id FROM session WHERE id = :sessionId")
    suspend fun getDeloadIdBySessionId(sessionId: Long): Long?

    @Query(
        """
        SELECT COUNT(*) FROM session
        WHERE deload_id = :deloadId AND status IN ('COMPLETED', 'INCOMPLETE')
        """,
    )
    suspend fun countDeloadSessions(deloadId: Long): Int

    @Query("SELECT id FROM session WHERE deload_id = :deloadId AND status IN ('COMPLETED', 'INCOMPLETE')")
    suspend fun getSessionIdsByDeloadId(deloadId: Long): List<Long>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session
            WHERE deload_id IS NULL
              AND status IN ('COMPLETED', 'INCOMPLETE')
              AND id > (SELECT MAX(id) FROM session WHERE deload_id = :deloadId)
        )
        """,
    )
    suspend fun hasSessionAfterDeload(deloadId: Long): Boolean

    @Query(
        """
        SELECT
            s.status,
            r.name AS routineName,
            rv.version_number AS versionNumber,
            r.id AS routineId,
            COALESCE(
                (SELECT SUM(es.weight_kg * es.reps)
                 FROM exercise_set es
                 INNER JOIN session_exercise se ON es.session_exercise_id = se.id
                 WHERE se.session_id = s.id),
                0.0
            ) AS totalTonnageKg,
            (SELECT COUNT(DISTINCT se.id)
             FROM session_exercise se
             WHERE se.session_id = s.id
               AND (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) > 0
            ) AS totalExercises,
            (SELECT COUNT(DISTINCT se2.id)
             FROM session_exercise se2
             WHERE se2.session_id = s.id
               AND se2.is_finalized = 1
               AND (SELECT COUNT(*) FROM exercise_set es2 WHERE es2.session_exercise_id = se2.id) > 0
            ) AS completedExercises
        FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE s.id = :sessionId
        """,
    )
    suspend fun getSessionSummaryInfo(sessionId: Long): SessionSummaryInfo

    @Query(
        """
        SELECT id, routine_version_id, deload_id, date, status
        FROM session
        WHERE status IN ('COMPLETED', 'INCOMPLETE')
        ORDER BY date ASC, id ASC
        """,
    )
    suspend fun getClosedSessionsOrdered(): List<SessionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM session
        WHERE status IN ('COMPLETED', 'INCOMPLETE')
          AND date >= :weekStartDate
          AND date <= :weekEndDate
        """,
    )
    suspend fun countSessionsInWeek(weekStartDate: String, weekEndDate: String): Int

    @Query("SELECT MIN(date) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')")
    suspend fun getFirstSessionDate(): String?

    @Query(
        """
        SELECT s.id FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        WHERE rv.routine_id = :routineId
          AND s.status IN ('COMPLETED', 'INCOMPLETE')
          AND s.deload_id IS NULL
        ORDER BY s.date DESC, s.id DESC
        LIMIT :limit
        """,
    )
    suspend fun getSessionIdsByRoutineInRange(routineId: Long, limit: Int): List<Long>

    @Query(
        """
        SELECT
            s.id AS sessionId,
            s.date,
            r.name AS routineName,
            rv.version_number AS versionNumber,
            s.status,
            COALESCE(
                (SELECT SUM(es.weight_kg * es.reps)
                 FROM exercise_set es
                 INNER JOIN session_exercise se ON es.session_exercise_id = se.id
                 WHERE se.session_id = s.id),
                0.0
            ) AS totalTonnageKg
        FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE s.status IN ('COMPLETED', 'INCOMPLETE')
        ORDER BY s.date DESC, s.id DESC
        """,
    )
    suspend fun getClosedSessionsWithSummary(): List<ClosedSessionDto>

    @Query(
        """
        SELECT MAX(s.date)
        FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        WHERE rv.routine_id = :routineId
          AND s.status IN ('COMPLETED', 'INCOMPLETE')
        """,
    )
    suspend fun getLastSessionDateByRoutine(routineId: Long): String?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session s
            INNER JOIN routine_version rv ON s.routine_version_id = rv.id
            WHERE rv.routine_id = :routineId
              AND s.status = 'IN_PROGRESS'
        )
        """,
    )
    suspend fun hasActiveSessionForRoutine(routineId: Long): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session
            WHERE routine_version_id = :routineVersionId
              AND status = 'IN_PROGRESS'
        )
        """,
    )
    suspend fun hasActiveSessionForVersion(routineVersionId: Long): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session
            WHERE routine_version_id = :routineVersionId
        )
        """,
    )
    suspend fun hasSessionsForVersion(routineVersionId: Long): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session s
            INNER JOIN routine_version rv ON s.routine_version_id = rv.id
            WHERE rv.routine_id = :routineId
        )
        """,
    )
    suspend fun hasSessionsForRoutine(routineId: Long): Boolean
}
