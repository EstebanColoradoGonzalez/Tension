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
}
