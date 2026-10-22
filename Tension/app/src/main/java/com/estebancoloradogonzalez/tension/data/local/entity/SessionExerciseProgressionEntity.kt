package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Progression classification of one **(session exercise, equipment) pair** (HU-40).
 *
 * `session_exercise.progression_classification` holds the *consolidated* reading of the
 * exercise — what the KPIs, the progression rate and the alerts consume. This table holds
 * the reading of each implement separately, which is what the post-session summary shows
 * as one line per pair.
 *
 * It is a dated, immutable fact and not a state, which is why it is persisted rather than
 * derived: deriving it at read time would give the summary and the KPIs two different
 * sources for the same classification.
 *
 * `CASCADE` on `session_exercise` because a discarded session already drags its
 * `exercise_set` rows the same way: this row is of the same order of datum as a set.
 */
@Entity(
    tableName = "session_exercise_progression",
    primaryKeys = ["session_exercise_id", "equipment_type_id"],
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
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
data class SessionExerciseProgressionEntity(
    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: Long,

    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,

    /** Null means *no history for this pair* — never a regression (CA-40.02). */
    @ColumnInfo(name = "progression_classification")
    val progressionClassification: String? = null,

    /** Load the progression engine prescribes for this pair after the session. */
    @ColumnInfo(name = "prescribed_load_kg")
    val prescribedLoadKg: Double? = null,

    /**
     * How this close read the pair against the 8–12 range: `TOP`, `WITHIN` or `BELOW`
     * (HU-46, CA-46.01). Null when the range rule did not apply — isometric exercises and
     * deload sessions — and the summary falls back to the signal it always showed.
     *
     * Persisted for the same reason as [prescribedLoadKg]: the summary is a faithful record
     * of this close, not a value later sessions may already have moved.
     */
    @ColumnInfo(name = "range_reading")
    val rangeReading: String? = null,

    /** Consecutive top-of-range sessions of the pair **after** this close: the «N de 4». */
    @ColumnInfo(name = "top_range_streak")
    val topRangeStreak: Int? = null,
)
