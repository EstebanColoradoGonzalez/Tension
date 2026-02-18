package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseProgressionDao {

    @Query("SELECT * FROM exercise_progression WHERE exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<ExerciseProgressionEntity?>

    @Insert
    suspend fun insert(progression: ExerciseProgressionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(progression: ExerciseProgressionEntity)

    @Update
    suspend fun update(progression: ExerciseProgressionEntity)

    @Query(
        """
        UPDATE exercise_progression
        SET status = 'IN_DELOAD'
        WHERE status NOT IN ('NO_HISTORY', 'MASTERED')
        """,
    )
    suspend fun transitionToDeload()

    @Query("SELECT * FROM exercise_progression WHERE status = 'IN_DELOAD'")
    suspend fun getAllInDeload(): List<ExerciseProgressionEntity>

    @Query("SELECT * FROM exercise_progression WHERE prescribed_load_kg IS NOT NULL")
    suspend fun getAllWithPrescribedLoad(): List<ExerciseProgressionEntity>
}
