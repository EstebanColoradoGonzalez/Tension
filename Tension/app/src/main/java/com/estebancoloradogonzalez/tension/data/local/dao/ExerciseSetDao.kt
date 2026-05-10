package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.domain.model.SetDistributionData
import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData

@Dao
interface ExerciseSetDao {

    @Insert
    suspend fun insert(set: ExerciseSetEntity): Long

    @Query(
        """
        SELECT COUNT(*) + 1 FROM exercise_set
        WHERE session_exercise_id = :sessionExerciseId
        """,
    )
    suspend fun getNextSetNumber(sessionExerciseId: Long): Int

    @Query(
        """
        SELECT es.weight_kg
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        INNER JOIN session s ON se.session_id = s.id
        WHERE COALESCE(se.original_exercise_id, se.exercise_id) = :exerciseId
          AND s.deload_id IS NULL
        ORDER BY es.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastWeightForExercise(exerciseId: Long): Double?

    @Query(
        """
        SELECT AVG(es.weight_kg)
        FROM exercise_set es
        WHERE es.session_exercise_id = (
            SELECT se.id
            FROM session_exercise se
            INNER JOIN session s ON se.session_id = s.id
            WHERE COALESCE(se.original_exercise_id, se.exercise_id) = :exerciseId
              AND s.deload_id IS NULL
              AND s.date <= :activationDate
              AND s.status IN ('COMPLETED', 'INCOMPLETE')
            ORDER BY s.date DESC, s.id DESC
            LIMIT 1
        )
        """,
    )
    suspend fun getPreDeloadAvgWeight(exerciseId: Long, activationDate: String): Double?

    @Query(
        """
        SELECT weight_kg AS weightKg, reps, rir
        FROM exercise_set
        WHERE session_exercise_id = :sessionExerciseId
        ORDER BY set_number
        """,
    )
    suspend fun getSetsForSessionExercise(sessionExerciseId: Long): List<ExerciseSetData>

    @Query(
        """
        SELECT es.weight_kg AS weightKg, es.reps, es.rir
        FROM exercise_set es
        WHERE es.session_exercise_id = (
            SELECT se2.id
            FROM session_exercise se2
            INNER JOIN session s2 ON se2.session_id = s2.id
            WHERE COALESCE(se2.original_exercise_id, se2.exercise_id) = :exerciseId
              AND s2.id != :currentSessionId
              AND s2.status IN ('COMPLETED', 'INCOMPLETE')
              AND s2.deload_id IS NULL
            ORDER BY s2.date DESC, s2.id DESC
            LIMIT 1
        )
        ORDER BY es.set_number
        """,
    )
    suspend fun getLastHistoricalSets(exerciseId: Long, currentSessionId: Long): List<ExerciseSetData>

    @Query(
        """
        SELECT es.weight_kg AS weightKg, es.reps, mz.muscle_group AS muscleGroup
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        INNER JOIN exercise_muscle_zone emz ON COALESCE(se.original_exercise_id, se.exercise_id) = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE se.session_id IN (:sessionIds)
        """,
    )
    suspend fun getTonnageDataBySessionIds(sessionIds: List<Long>): List<SetTonnageData>

    @Query(
        """
        SELECT es.rir FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.session_id IN (:sessionIds)
        """,
    )
    suspend fun getRirValuesBySessionIds(sessionIds: List<Long>): List<Int>

    @Query(
        """
        SELECT AVG(es.weight_kg)
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE COALESCE(se.original_exercise_id, se.exercise_id) = :exerciseId
          AND se.session_id = :sessionId
        """,
    )
    suspend fun getAvgWeightByExerciseInSession(exerciseId: Long, sessionId: Long): Double?

    @Query(
        """
        SELECT mz.name AS muscleZoneName, mz.muscle_group AS muscleGroup, COUNT(*) AS setCount
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        INNER JOIN exercise_muscle_zone emz ON COALESCE(se.original_exercise_id, se.exercise_id) = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE se.session_id IN (:sessionIds)
        GROUP BY mz.name, mz.muscle_group
        """,
    )
    suspend fun getSetDistributionBySessionIds(sessionIds: List<Long>): List<SetDistributionData>
}

data class ExerciseSetData(
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
)
