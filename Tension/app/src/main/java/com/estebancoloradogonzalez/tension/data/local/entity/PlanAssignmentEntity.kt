package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Asignación de un ejercicio a una versión de rutina del plan.
 *
 * Desde HU-41 lleva el **equipamiento sugerido**: el implemento que el plan propone para
 * ese puesto. La sugerencia *sugiere, no impone* —el ejecutante puede cambiarla al
 * registrar la serie— y cada ejercicio de un puesto dual lleva la suya, porque son
 * ejercicios distintos con opciones distintas.
 */
@Entity(
    tableName = "plan_assignment",
    primaryKeys = ["routine_version_id", "exercise_id"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_version_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["suggested_equipment_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["exercise_id"]),
        Index(value = ["suggested_equipment_type_id"]),
    ],
)
data class PlanAssignmentEntity(
    @ColumnInfo(name = "routine_version_id")
    val routineVersionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "sets")
    val sets: Int,

    @ColumnInfo(name = "reps")
    val reps: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "slot", defaultValue = "0")
    val slot: Int = 0,

    /**
     * Implemento que el plan sugiere para este puesto. **Obligatorio**: ninguna asignación
     * puede existir sin él (CA-41.07), ni al sembrar ni al asignar a mano (CA-41.08).
     *
     * Debe ser una de las opciones que el ejercicio admite en `exercise_equipment`. La
     * invariante se valida en el caso de uso y **no** con una FK compuesta: `RESTRICT`
     * sobre una tabla que la ficha del ejercicio edita en caliente convertiría un rechazo
     * ya explicado con palabras en una excepción de SQLite con peor mensaje.
     *
     * Sin `defaultValue` por el mismo argumento que `exercise_muscle_zone.is_primary`.
     */
    @ColumnInfo(name = "suggested_equipment_type_id")
    val suggestedEquipmentTypeId: Long,
)
