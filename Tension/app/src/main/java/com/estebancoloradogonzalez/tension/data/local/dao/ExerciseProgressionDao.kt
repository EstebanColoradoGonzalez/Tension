package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Access to the progression state, which since HU-40 is keyed by the
 * **(exercise, equipment) pair**.
 *
 * Every read that used to resolve on the exercise alone now names the implement. The only
 * exceptions are the two bulk reads used by the deload protocol, which operate over all
 * pairs by design.
 */
@Dao
interface ExerciseProgressionDao {

    @Query(
        """
        SELECT * FROM exercise_progression
        WHERE exercise_id = :exerciseId AND equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun getByPair(exerciseId: Long, equipmentTypeId: Long): ExerciseProgressionEntity?

    /**
     * Every pair of the exercise, in catalog order.
     *
     * This is the input of the consolidated reading (`ProgressionConsolidationRule`): the
     * exercise progresses if any of these progressed, and plateaus only if all of them
     * stalled.
     */
    @Query(
        """
        SELECT * FROM exercise_progression
        WHERE exercise_id = :exerciseId
        ORDER BY equipment_type_id ASC
        """,
    )
    suspend fun getAllByExercise(exerciseId: Long): List<ExerciseProgressionEntity>

    @Query(
        """
        SELECT * FROM exercise_progression
        WHERE exercise_id = :exerciseId
        ORDER BY equipment_type_id ASC
        """,
    )
    fun observeAllByExercise(exerciseId: Long): Flow<List<ExerciseProgressionEntity>>

    /**
     * Pairs of the exercise with their implement name and accumulated counter, in catalog
     * order.
     *
     * The plateau alert has to name which implements are stalled to be actionable
     * (CA-40.05), and it derives them from here instead of persisting them: the counters
     * can move between the moment the alert is raised and the moment it is read, and what
     * the executant needs to see is today's state.
     */
    @Query(
        """
        SELECT
            ep.equipment_type_id AS equipmentTypeId,
            et.name AS equipmentTypeName,
            ep.status AS status,
            ep.sessions_without_progression AS sessionsWithoutProgression
        FROM exercise_progression ep
        INNER JOIN equipment_type et ON ep.equipment_type_id = et.id
        WHERE ep.exercise_id = :exerciseId
        ORDER BY ep.equipment_type_id ASC
        """,
    )
    suspend fun getPairStatesByExercise(exerciseId: Long): List<ExercisePairStateDto>

    @Insert
    suspend fun insert(progression: ExerciseProgressionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(progression: ExerciseProgressionEntity)

    @Update
    suspend fun update(progression: ExerciseProgressionEntity)

    /**
     * Freezes every pair of the given exercises for the deload microcycle.
     *
     * Applies to pairs and not to exercises: a deload reduces the load of each implement
     * over its own baseline, and a pair with no history has no load to reduce.
     */
    @Query(
        """
        UPDATE exercise_progression
        SET status = 'IN_DELOAD'
        WHERE status NOT IN ('NO_HISTORY', 'MASTERED')
        AND exercise_id IN (:exerciseIds)
        """,
    )
    suspend fun transitionToDeload(exerciseIds: List<Long>)

    @Query("SELECT * FROM exercise_progression WHERE status = 'IN_DELOAD'")
    suspend fun getAllInDeload(): List<ExerciseProgressionEntity>

    @Query("SELECT * FROM exercise_progression WHERE prescribed_load_kg IS NOT NULL")
    suspend fun getAllWithPrescribedLoad(): List<ExerciseProgressionEntity>
}

/** One pair of an exercise, as the alert detail needs to name it. */
data class ExercisePairStateDto(
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val status: String,
    val sessionsWithoutProgression: Int,
)
