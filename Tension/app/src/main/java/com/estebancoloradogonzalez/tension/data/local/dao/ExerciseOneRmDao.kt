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
 * Write the pair, remove it, and list everything for the screen. The table is written only
 * through `OneRmRecordWriter` — from the set registration and correction transactions, the
 * change of an exercise's body-weight percentage and the registration of a body weight
 * (HU-49) — and read from the 1RM screen; **no other query in the system names it**.
 */
@Dao
interface ExerciseOneRmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(oneRm: ExerciseOneRmEntity)

    /**
     * Removes the record of the pair. Only the rebuild calls it (HU-47, CA-47.11; HU-49,
     * CA-49.08): when no set of the pair qualifies any more, the pair no longer has a real
     * value, and CA-42.07 says such a pair is not shown — neither as a zero nor as
     * a dash. The table is the list, so the row goes.
     */
    @Query(
        """
        DELETE FROM exercise_one_rm
        WHERE exercise_id = :exerciseId AND equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun deleteByPair(exerciseId: Long, equipmentTypeId: Long)

    /**
     * Every pair with a record, with the names the screen shows.
     *
     * Ordered here and not in Kotlin: alphabetically by exercise (CA-42.02) and then by
     * implement `id`, which is the declared position of the equipment catalog — the same
     * order HU-39 fixed for the checkbox list, and the reason `EquipmentTypeDao` also orders
     * by `id` rather than by name.
     *
     * Each row carries the date the record was reached and the body weight in force on that
     * date — the latest entry dated on or before it, the one saved last on a tie, as in
     * `WeightRecordDao.getLatestWeight` — which the relative strength divides by (HU-49,
     * CA-49.03, CA-49.06). Reading `weight_record` here also makes Room re-emit the list when
     * a body weight is recorded.
     */
    @Query(
        """
        SELECT
            r.exercise_id AS exerciseId,
            e.name AS exerciseName,
            r.equipment_type_id AS equipmentTypeId,
            et.name AS equipmentTypeName,
            r.one_rm_kg AS oneRmKg,
            r.achieved_on AS achievedOn,
            (SELECT wr.weight_kg FROM weight_record wr
             WHERE wr.date <= r.achieved_on
             ORDER BY wr.date DESC, wr.id DESC LIMIT 1) AS bodyWeightKg
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
    val achievedOn: String,
    val bodyWeightKg: Double?,
)
