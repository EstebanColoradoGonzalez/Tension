package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert
    suspend fun insert(alert: AlertEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM alert
            WHERE exercise_id = :exerciseId AND type = :type AND is_active = 1
        )
        """,
    )
    suspend fun existsActiveByExercise(exerciseId: Long, type: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM alert
            WHERE module_code = :moduleCode AND type = :type AND is_active = 1
        )
        """,
    )
    suspend fun existsActiveByModule(moduleCode: String, type: String): Boolean

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE exercise_id = :exerciseId AND type = :type AND is_active = 1
        """,
    )
    suspend fun resolveByExerciseAndType(exerciseId: Long, type: String, resolvedAt: String)

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE module_code = :moduleCode AND type = :type AND is_active = 1
        """,
    )
    suspend fun resolveByModuleAndType(moduleCode: String, type: String, resolvedAt: String)

    @Query("SELECT COUNT(*) FROM alert WHERE is_active = 1")
    fun countActive(): Flow<Int>

    @Query("SELECT * FROM alert WHERE is_active = 1 ORDER BY level ASC, created_at DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query(
        """
        SELECT * FROM alert
        WHERE type = :type AND is_active = 1
        ORDER BY created_at DESC
        """,
    )
    suspend fun getActiveAlertsByType(type: String): List<AlertEntity>

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE type = :type AND is_active = 1
        """,
    )
    suspend fun resolveAllByType(type: String, resolvedAt: String)

    @Query("SELECT * FROM alert WHERE id = :alertId")
    suspend fun getAlertById(alertId: Long): AlertEntity?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM alert
            WHERE muscle_group = :muscleGroup AND type = :type AND is_active = 1
        )
        """,
    )
    suspend fun existsActiveByMuscleGroup(muscleGroup: String, type: String): Boolean

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE muscle_group = :muscleGroup AND type = :type AND is_active = 1
        """,
    )
    suspend fun resolveByMuscleGroupAndType(
        muscleGroup: String,
        type: String,
        resolvedAt: String,
    )
}
