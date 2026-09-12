package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercise",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["exercise_id"]),
        Index(value = ["session_id", "exercise_id"], unique = true),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long?,

    @ColumnInfo(name = "progression_classification")
    val progressionClassification: String? = null,

    @ColumnInfo(name = "is_finalized", defaultValue = "0")
    val isFinalized: Int = 0,

    @ColumnInfo(name = "pending_selection", defaultValue = "0")
    val pendingSelection: Int = 0,

    @ColumnInfo(name = "slot", defaultValue = "0")
    val slot: Int = 0,

    /**
     * Si el ejercicio lo añadió el ejecutante a la sesión en curso, en lugar de traerlo el
     * plan (HU-43).
     *
     * El añadido es un `session_exercise` **más**, sin ejercicio de origen: HU-34 retiró la
     * sustitución por grupo muscular justamente porque mantenía dos identidades del mismo
     * ejercicio dentro de la sesión, y cada consulta de progresión pagaba esa ambigüedad.
     * Aquí no hay segunda identidad — hay una marca.
     *
     * La marca existe para **ordenar, señalar y contar el presupuesto de retiro**, nunca
     * para excluir: las series del añadido cuentan para la progresión, el tonelaje, los KPIs
     * y el 1RM exactamente como las demás (CA-43.08). Ninguna consulta de agregación debe
     * filtrar por esta columna.
     */
    @ColumnInfo(name = "is_extra", defaultValue = "0")
    val isExtra: Int = 0,

    /**
     * Series que la sesión prescribe a este ejercicio, cuando no las prescribe el plan.
     *
     * `NULL` en las filas que trajo el plan: las suyas viven en `plan_assignment`. El
     * añadido no tiene asignación de la que colgar, y su prescripción por defecto —3 series
     * (CA-43.03)— se persiste en vez de resolverse con un `CASE` repetido en cada consulta
     * que la necesita. Con ello `SessionAdjustmentRule` es el único sitio que la decide, y
     * la fila queda diciendo con qué se comprometió el ejecutante.
     */
    @ColumnInfo(name = "prescribed_sets")
    val prescribedSets: Int? = null,

    /**
     * Rango de repeticiones propio del ejercicio añadido. `NULL` en las filas del plan.
     *
     * Usa el mismo vocabulario que `plan_assignment.reps` —`8-12`, `30-45_SEC`,
     * `TO_TECHNICAL_FAILURE`—: el añadido nace con un rango del plan, no con uno propio.
     */
    @ColumnInfo(name = "prescribed_reps")
    val prescribedReps: String? = null,
)
