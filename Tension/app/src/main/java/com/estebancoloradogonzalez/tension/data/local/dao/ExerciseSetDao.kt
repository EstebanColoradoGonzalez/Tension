package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity

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
        WHERE se.exercise_id = :exerciseId
        ORDER BY es.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastWeightForExercise(exerciseId: Long): Double?

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
            WHERE se2.exercise_id = :exerciseId
              AND s2.id != :currentSessionId
              AND s2.status IN ('COMPLETED', 'INCOMPLETE')
            ORDER BY s2.date DESC, s2.id DESC
            LIMIT 1
        )
        ORDER BY es.set_number
        """,
    )
    suspend fun getLastHistoricalSets(exerciseId: Long, currentSessionId: Long): List<ExerciseSetData>
}

data class ExerciseSetData(
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
)
