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
}
