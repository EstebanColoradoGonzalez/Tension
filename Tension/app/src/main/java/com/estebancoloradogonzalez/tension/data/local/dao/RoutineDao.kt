package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routine ORDER BY sort_order ASC")
    fun getAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine ORDER BY sort_order ASC")
    suspend fun getAllOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT MAX(sort_order) FROM routine")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM routine")
    suspend fun countRoutines(): Int

    @Query("UPDATE routine SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM routine WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM routine WHERE name = :name AND id != :excludeId)")
    suspend fun existsByNameExcluding(name: String, excludeId: Long): Boolean

    @Query("UPDATE routine SET movement_category = :category WHERE id = :id")
    suspend fun updateMovementCategory(id: Long, category: String)

    /**
     * Ejercicios asignados a la rutina en cualquiera de sus versiones, como puesto o como
     * alternativa, con su categoría (CA-46.14). Cambiar la categoría de la rutina los
     * contradiría a todos los que no compartan la nueva.
     */
    @Query(
        """
        SELECT DISTINCT e.name AS name, e.movement_category AS category, 0 AS sortOrder
        FROM plan_assignment pa
        INNER JOIN routine_version rv ON pa.routine_version_id = rv.id
        INNER JOIN exercise e ON pa.exercise_id = e.id
        WHERE rv.routine_id = :routineId
        ORDER BY e.name ASC
        """,
    )
    suspend fun getExercisesAssignedToRoutine(routineId: Long): List<CategorizedNameDto>

    /** Categoría de la rutina a la que pertenece la versión: la que acota su plan (CA-46.12). */
    @Query(
        """
        SELECT r.movement_category FROM routine_version rv
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE rv.id = :routineVersionId
        """,
    )
    suspend fun getMovementCategoryOfVersion(routineVersionId: Long): String?

    /**
     * Categoría del día: la de la rutina de la sesión (CA-46.09). La reasignación temporal ya
     * se agotó al elegir la versión con la que nació la sesión, así que no hace falta mirarla.
     */
    @Query(
        """
        SELECT r.movement_category FROM session s
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE s.id = :sessionId
        """,
    )
    suspend fun getMovementCategoryOfSession(sessionId: Long): String?
}
