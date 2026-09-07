package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseProgressionEntity

/**
 * Per-pair classification of a session (HU-40).
 *
 * `session_exercise.progression_classification` keeps the consolidated reading that the
 * KPIs and the alerts consume; this table keeps what each implement did, which is what the
 * post-session summary presents as one line per pair.
 */
@Dao
interface SessionExerciseProgressionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SessionExerciseProgressionEntity)

    @Query("DELETE FROM session_exercise_progression WHERE session_exercise_id = :sessionExerciseId")
    suspend fun deleteBySessionExerciseId(sessionExerciseId: Long)

    @Query(
        """
        SELECT * FROM session_exercise_progression
        WHERE session_exercise_id = :sessionExerciseId
        ORDER BY equipment_type_id ASC
        """,
    )
    suspend fun getBySessionExerciseId(sessionExerciseId: Long): List<SessionExerciseProgressionEntity>

    /**
     * One row per (session exercise, implement) trained in the session, with everything the
     * summary needs to build its line.
     *
     * The prescribed load comes from this table and not from `exercise_progression` on
     * purpose: it is the load the engine decided **at this close**, and reading the live
     * state would show a value later sessions may already have moved.
     *
     * `previousTotalReps` resolves the previous session **of the same pair**, not of the
     * exercise: a session in which the exercise was trained only with another implement is
     * not the pair's previous session, and treating it as such would compare a dumbbell
     * against a cable through the back door.
     */
    @Query(
        """
        SELECT
            sep.session_exercise_id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            sep.equipment_type_id AS equipmentTypeId,
            et.name AS equipmentTypeName,
            sep.progression_classification AS classification,
            sep.prescribed_load_kg AS prescribedLoadKg,
            COALESCE((
                SELECT AVG(es.weight_kg) FROM exercise_set es
                WHERE es.session_exercise_id = se.id
                  AND es.equipment_type_id = sep.equipment_type_id
            ), 0.0) AS avgWeightKg,
            COALESCE((
                SELECT SUM(es.reps) FROM exercise_set es
                WHERE es.session_exercise_id = se.id
                  AND es.equipment_type_id = sep.equipment_type_id
            ), 0) AS totalReps,
            (
                SELECT COUNT(*) FROM exercise_set es
                WHERE es.session_exercise_id = se.id
                  AND es.equipment_type_id = sep.equipment_type_id
            ) AS setCount,
            (
                SELECT SUM(es3.reps)
                FROM exercise_set es3
                WHERE es3.equipment_type_id = sep.equipment_type_id
                  AND es3.session_exercise_id = (
                      SELECT se3.id
                      FROM session_exercise se3
                      INNER JOIN session s3 ON se3.session_id = s3.id
                      INNER JOIN exercise_set es4 ON es4.session_exercise_id = se3.id
                      WHERE se3.exercise_id = se.exercise_id
                        AND s3.id != :sessionId
                        AND s3.status IN ('COMPLETED', 'INCOMPLETE')
                        AND s3.deload_id IS NULL
                        AND es4.equipment_type_id = sep.equipment_type_id
                      ORDER BY s3.date DESC, s3.id DESC
                      LIMIT 1
                  )
            ) AS previousTotalReps
        FROM session_exercise_progression sep
        INNER JOIN session_exercise se ON sep.session_exercise_id = se.id
        INNER JOIN equipment_type et ON sep.equipment_type_id = et.id
        WHERE se.session_id = :sessionId
        ORDER BY se.slot ASC, sep.equipment_type_id ASC
        """,
    )
    suspend fun getPairSummariesForSession(sessionId: Long): List<SessionPairSummaryDto>
}

/** One (session exercise, implement) line of the post-session summary. */
data class SessionPairSummaryDto(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val classification: String?,
    val prescribedLoadKg: Double?,
    val avgWeightKg: Double,
    val totalReps: Int,
    val setCount: Int,
    val previousTotalReps: Int?,
)
