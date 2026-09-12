package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseOneRmEntity
import kotlinx.coroutines.flow.Flow

/**
 * Access to the estimated 1RM, keyed by the **(exercise, equipment) pair** (HU-42).
 *
 * Three operations and no more: read one value to compare against, write the pair, and list
 * everything for the screen. The table is written from the set registration transaction and
 * read from the 1RM screen; **no other query in the system names it**.
 */
@Dao
interface ExerciseOneRmDao {

    /**
     * The record currently held by the pair, or null if the pair has none.
     *
     * This is the left-hand side of the monotonicity comparison (CA-42.04). It is a read and
     * not an `UPDATE … MAX(…)` because the upsert syntax that would fold both into one
     * statement needs SQLite 3.24 — API 30 — and the project targets API 26. The comparison
     * is safe regardless: it runs inside the transaction that is already open around the set.
     */
    @Query(
        """
        SELECT one_rm_kg FROM exercise_one_rm
        WHERE exercise_id = :exerciseId AND equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun getValue(exerciseId: Long, equipmentTypeId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(oneRm: ExerciseOneRmEntity)

    /**
     * Every pair with a record, with the names the screen shows.
     *
     * Ordered here and not in Kotlin: alphabetically by exercise (CA-42.02) and then by
     * implement `id`, which is the declared position of the equipment catalog — the same
     * order HU-39 fixed for the checkbox list, and the reason `EquipmentTypeDao` also orders
     * by `id` rather than by name.
     */
    @Query(
        """
        SELECT
            r.exercise_id AS exerciseId,
            e.name AS exerciseName,
            r.equipment_type_id AS equipmentTypeId,
            et.name AS equipmentTypeName,
            r.one_rm_kg AS oneRmKg
        FROM exercise_one_rm r
        INNER JOIN exercise e ON e.id = r.exercise_id
        INNER JOIN equipment_type et ON et.id = r.equipment_type_id
        ORDER BY e.name ASC, r.equipment_type_id ASC
        """,
    )
    fun getAll(): Flow<List<ExerciseOneRmDto>>
}

/** One (exercise, equipment) pair with its record, as the 1RM screen needs to name it. */
data class ExerciseOneRmDto(
    val exerciseId: Long,
    val exerciseName: String,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val oneRmKg: Double,
)
