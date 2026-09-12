package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Estimated 1RM of one **(exercise, equipment) pair** (HU-42).
 *
 * The weight handled in a movement is not comparable across implements — the same reason
 * that made the pair the unit of comparison in HU-40 — so a single figure per exercise
 * would mix magnitudes that are not the same strength. Hence the composite key.
 *
 * **The table is the list.** A row is born the first time a set qualifies, and only then:
 * exactly ten repetitions with an RIR of exactly one, over external load. Listing the rows
 * is therefore listing what CA-42.02 and CA-42.07 describe — trained exercises, trained
 * implements, never a zero and never a dash. Nothing has to be filtered out at read time.
 *
 * **A derived value that is nevertheless persisted.** The base convention says computed
 * values are not stored; this is the declared exception. The 1RM is a *record*: the maximum
 * of the qualifying sets over the whole history. Recomputing it from `exercise_set` could
 * lose a maximum reached in a set that a purge or a partial restore no longer carries, and
 * CA-42.08 requires a restored backup to reproduce it **without recalculating**.
 *
 * There is **no `calculated_at`**. `tree_state` keeps one because it makes the order of its
 * recalculation against the day sweep auditable; here there is no sweep, no order to audit,
 * and CA-42.06 states the view exposes the number and nothing else. A column no one reads,
 * and which repeating an identical session would still touch, is worse than its absence.
 *
 * **The dependency is strictly one-way.** This table is written when a set is registered and
 * read by the 1RM screen only. No rule of the decision engine, no alert and no KPI consults
 * it — same isolation model HU-37 established for the tree.
 */
@Entity(
    tableName = "exercise_one_rm",
    primaryKeys = ["exercise_id", "equipment_type_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["equipment_type_id"]),
    ],
)
data class ExerciseOneRmEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,

    /**
     * The record itself, in kilograms. Canonical unit of the system: the view derives pounds
     * on the fly with the fixed factor, and the capture unit of the originating set plays no
     * part in either storage or presentation (CA-42.06).
     */
    @ColumnInfo(name = "one_rm_kg")
    val oneRmKg: Double,
)
