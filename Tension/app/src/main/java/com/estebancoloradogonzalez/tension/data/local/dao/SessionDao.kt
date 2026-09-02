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
    /** Series registradas. Cero significa que la sesión puede cancelarse sin cerrarla. */
    val registeredSets: Int,
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

    /**
     * La sesión en curso que quedó de un día anterior, si la hay.
     *
     * Pasada la medianoche ya no puede continuarse: pertenece a un día que terminó, y mientras
     * siga viva tapa la propuesta del día nuevo con la tarjeta de reanudar.
     */
    @Query("SELECT id FROM session WHERE status = 'IN_PROGRESS' AND date < :today LIMIT 1")
    suspend fun getStaleActiveSessionId(today: String): Long?

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
            ) AS completedExercises,
            (SELECT COUNT(*) FROM exercise_set es
             INNER JOIN session_exercise se3 ON es.session_exercise_id = se3.id
             WHERE se3.session_id = s.id
            ) AS registeredSets
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

    /**
     * Total de sesiones registradas, sin distinguir rutina.
     *
     * Cuenta el esfuerzo real: `COMPLETED` e `INCOMPLETE`, nunca `IN_PROGRESS`. Cerrar una
     * sesión sin ninguna serie la descarta en lugar de persistirla, así que toda fila contada
     * aquí implica al menos una serie registrada.
     *
     * Incluye las sesiones de descarga: una descarga es entrenamiento registrado.
     */
    @Query("SELECT COUNT(*) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')")
    suspend fun countClosedSessions(): Int

    /**
     * Fecha de la última sesión registrada, sin distinguir rutina.
     *
     * Es la contraparte **global** de [getLastSessionDateByRoutine], que filtra por rutina
     * porque `ROUTINE_INACTIVITY` mide la inactividad de cada rutina por separado. Son medidas
     * distintas a propósito y no deben acoplarse.
     */
    @Query("SELECT MAX(date) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')")
    suspend fun getLastClosedSessionDate(): String?

    /** Si el ejecutante ya cerró una sesión en [date]. Resuelve el día como entrenado. */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session
            WHERE status IN ('COMPLETED', 'INCOMPLETE') AND date = :date
        )
        """,
    )
    fun hasClosedSessionOn(date: String): Flow<Boolean>

    /**
     * Borra la sesión. `session_exercise` y `exercise_set` caen por CASCADE.
     *
     * Solo se usa para descartar una sesión que se cierra sin ninguna serie registrada: no
     * hubo entrenamiento, y persistirla como INCOMPLETE la haría contar en la adherencia y
     * silenciaría la alerta de inactividad de su rutina.
     */
    @Query("DELETE FROM session WHERE id = :sessionId")
    suspend fun delete(sessionId: Long)

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
