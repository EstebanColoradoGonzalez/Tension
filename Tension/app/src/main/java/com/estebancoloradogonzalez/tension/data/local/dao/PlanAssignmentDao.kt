package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.PlanAssignmentEntity
import kotlinx.coroutines.flow.Flow

data class PlanAssignmentWithExerciseDetails(
    val exerciseId: Long,
    val exerciseName: String,
    /** Implementos admitidos, separados por [AGGREGATE_SEPARATOR] y en orden de catálogo. */
    val equipmentTypes: String?,
    /** Zonas que ejecutan el movimiento, separadas por [AGGREGATE_SEPARATOR]. Nunca vacía. */
    val primaryMuscleZones: String?,
    /** Zonas que asisten, separadas por [AGGREGATE_SEPARATOR]. Puede venir nula. */
    val secondaryMuscleZones: String?,
    /** Implemento que el plan sugiere para este puesto. Nunca nulo (CA-41.07). */
    val suggestedEquipmentTypeId: Long,
    val suggestedEquipmentName: String,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val isCustom: Int,
    val slot: Int,
)

data class SessionPreviewExerciseDto(
    val exerciseId: Long,
    val exerciseName: String,
    /** Implementos admitidos, separados por [AGGREGATE_SEPARATOR] y en orden de catálogo. */
    val equipmentTypes: String?,
    /**
     * Todas las zonas del ejercicio, principales primero. La vista previa de la sesión no
     * distingue jerarquía: enseña de qué va el entrenamiento, no cómo está catalogado.
     */
    val muscleZones: String?,
    /** Implemento que el plan sugiere para este puesto. Nunca nulo (CA-41.07). */
    val suggestedEquipmentTypeId: Long,
    val suggestedEquipmentName: String,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val prescribedLoadKg: Double?,
    val muscleGroup: String?,
    val slot: Int,
)

@Dao
interface PlanAssignmentDao {

    @Query("SELECT * FROM plan_assignment WHERE routine_version_id = :routineVersionId ORDER BY sort_order ASC")
    fun getByRoutineVersionId(routineVersionId: Long): Flow<List<PlanAssignmentEntity>>

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id AND emz.is_primary = 1
                ORDER BY mz.sort_order
            )) AS primaryMuscleZones,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id AND emz.is_primary = 0
                ORDER BY mz.sort_order
            )) AS secondaryMuscleZones,
            pa.suggested_equipment_type_id AS suggestedEquipmentTypeId,
            (SELECT et2.name FROM equipment_type et2
             WHERE et2.id = pa.suggested_equipment_type_id) AS suggestedEquipmentName,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        WHERE pa.routine_version_id = :routineVersionId
        GROUP BY e.id
        ORDER BY pa.sort_order ASC
        """,
    )
    fun getDetailsByRoutineVersionId(routineVersionId: Long): Flow<List<PlanAssignmentWithExerciseDetails>>

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id
                ORDER BY emz.is_primary DESC, mz.sort_order
            )) AS muscleZones,
            pa.suggested_equipment_type_id AS suggestedEquipmentTypeId,
            (SELECT et2.name FROM equipment_type et2
             WHERE et2.id = pa.suggested_equipment_type_id) AS suggestedEquipmentName,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            (SELECT MAX(ep.prescribed_load_kg) FROM exercise_progression ep
             WHERE ep.exercise_id = e.id) AS prescribedLoadKg,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = e.id
             ORDER BY emz2.is_primary DESC, mz2.sort_order ASC
             LIMIT 1) AS muscleGroup,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        WHERE pa.routine_version_id = :routineVersionId
        GROUP BY e.id
        ORDER BY pa.sort_order ASC
        """,
    )
    fun getPreviewByRoutineVersionId(routineVersionId: Long): Flow<List<SessionPreviewExerciseDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<PlanAssignmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assignment: PlanAssignmentEntity)

    @Query("DELETE FROM plan_assignment WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId")
    suspend fun delete(routineVersionId: Long, exerciseId: Long)

    @Query("DELETE FROM plan_assignment WHERE routine_version_id = :routineVersionId AND slot = :slot")
    suspend fun deleteBySlot(routineVersionId: Long, slot: Int)

    @Query("SELECT MAX(sort_order) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getMaxSortOrder(routineVersionId: Long): Int?

    @Query("SELECT MAX(slot) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getMaxSlot(routineVersionId: Long): Int?

    @Query(
        """
        SELECT 
            e.id AS exerciseId,
            e.name AS exerciseName,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id AND emz.is_primary = 1
                ORDER BY mz.sort_order
            )) AS primaryMuscleZones,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id AND emz.is_primary = 0
                ORDER BY mz.sort_order
            )) AS secondaryMuscleZones,
            pa.suggested_equipment_type_id AS suggestedEquipmentTypeId,
            (SELECT et2.name FROM equipment_type et2
             WHERE et2.id = pa.suggested_equipment_type_id) AS suggestedEquipmentName,
            pa.sets,
            pa.reps,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            pa.slot
        FROM plan_assignment pa
        INNER JOIN exercise e ON pa.exercise_id = e.id
        WHERE pa.routine_version_id = :routineVersionId AND pa.slot = :slot
        GROUP BY e.id
        ORDER BY e.name ASC
        """,
    )
    suspend fun getAlternativesForSlot(routineVersionId: Long, slot: Int): List<PlanAssignmentWithExerciseDetails>

    @Query("SELECT COUNT(*) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun countExercisesForRoutineVersion(routineVersionId: Long): Int

    /** Counts the number of distinct slots in a routine version (one slot = one exercise position). */
    @Query("SELECT COUNT(DISTINCT slot) FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun countDistinctSlots(routineVersionId: Long): Int

    @Query("SELECT exercise_id FROM plan_assignment WHERE routine_version_id = :routineVersionId")
    suspend fun getExerciseIdsByRoutineVersionId(routineVersionId: Long): List<Long>

    /**
     * Counts the number of distinct SLOTS that have at least one exercise either in a
     * plateau or with a REGRESSION classification in this session. Uses slots as the unit
     * so that the presence of alternatives does not dilute the affected ratio.
     *
     * Since HU-40 an exercise counts as stalled only when **every** one of its pairs is in
     * a plateau — the conjunction of CA-40.05. Reading `ep.status = 'IN_PLATEAU'` through a
     * join would mark the slot as soon as *any* implement stalled, which is the disjunction
     * the criterion explicitly rules out for plateaus: one implement still progressing
     * contradicts the plateau, and a deload must not be recommended on a contradiction.
     *
     * The `EXISTS` clause is not redundant with the `NOT EXISTS`: an exercise that has
     * never been trained has no pairs at all, and an empty conjunction would otherwise
     * declare it stalled.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT pa.slot)
        FROM plan_assignment pa
        LEFT JOIN session_exercise se ON (
            se.session_id = :sessionId
            AND se.exercise_id = pa.exercise_id
        )
        WHERE pa.routine_version_id = :routineVersionId
        AND (
            (
                EXISTS (
                    SELECT 1 FROM exercise_progression ep
                    WHERE ep.exercise_id = pa.exercise_id
                )
                AND NOT EXISTS (
                    SELECT 1 FROM exercise_progression ep
                    WHERE ep.exercise_id = pa.exercise_id
                      AND ep.status <> 'IN_PLATEAU'
                )
            )
            OR se.progression_classification = 'REGRESSION'
        )
        """,
    )
    suspend fun countAffectedSlotsForDeload(routineVersionId: Long, sessionId: Long): Int

    @Query(
        """
        SELECT DISTINCT mz.name
        FROM plan_assignment pa
        INNER JOIN exercise_muscle_zone emz ON pa.exercise_id = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        INNER JOIN routine_version rv ON pa.routine_version_id = rv.id
        WHERE rv.routine_id = :routineId
        ORDER BY mz.sort_order ASC
        """,
    )
    suspend fun getMuscleZoneNamesByRoutineId(routineId: Long): List<String>

    @Query(
        """
        UPDATE plan_assignment
        SET sets = :sets, reps = :reps
        WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId
        """,
    )
    suspend fun updateSetsAndReps(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)

    @Query(
        """
        UPDATE plan_assignment
        SET sets = :sets, reps = :reps
        WHERE routine_version_id = :routineVersionId AND slot = :slot
        """,
    )
    suspend fun updateSetsAndRepsBySlot(routineVersionId: Long, slot: Int, sets: Int, reps: String)

    @Query(
        """
        UPDATE plan_assignment
        SET suggested_equipment_type_id = :equipmentTypeId
        WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId
        """,
    )
    suspend fun updateSuggestedEquipment(
        routineVersionId: Long,
        exerciseId: Long,
        equipmentTypeId: Long,
    )

    @Query(
        """
        SELECT pa.suggested_equipment_type_id
        FROM plan_assignment pa
        WHERE pa.routine_version_id = :routineVersionId AND pa.exercise_id = :exerciseId
        """,
    )
    suspend fun getSuggestedEquipment(routineVersionId: Long, exerciseId: Long): Long?

    /**
     * Sugerencia del plan para el ejercicio en la **versión vigente** de la rutina de la
     * sesión. Es el segundo nivel de precedencia del selector de serie (CA-41.05): manda
     * sobre la primera opción admitida, y cede ante el implemento ya usado en la sesión.
     *
     * Devuelve nulo cuando el ejercicio no tiene asignación —el añadido dentro de la
     * sesión— y entonces la precedencia cae a CA-39.04.
     */
    @Query(
        """
        SELECT pa.suggested_equipment_type_id
        FROM plan_assignment pa
        INNER JOIN session s ON s.routine_version_id = pa.routine_version_id
        WHERE s.id = :sessionId AND pa.exercise_id = :exerciseId
        """,
    )
    suspend fun getSuggestedEquipmentForSession(sessionId: Long, exerciseId: Long): Long?

    /**
     * Rutinas cuyo plan sugiere ese implemento para ese ejercicio.
     *
     * Sostiene la dirección inversa de CA-41.08: retirar de un ejercicio una opción que
     * alguna asignación tiene como sugerencia queda impedido mientras esa asignación
     * exista. Devuelve los **nombres** y no un conteo para que el rechazo pueda nombrar la
     * rutina — un «no se puede» sin decir por qué obliga al ejecutante a adivinar.
     */
    @Query(
        """
        SELECT DISTINCT r.name
        FROM plan_assignment pa
        INNER JOIN routine_version rv ON pa.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        WHERE pa.exercise_id = :exerciseId AND pa.suggested_equipment_type_id = :equipmentTypeId
        ORDER BY r.sort_order ASC
        """,
    )
    suspend fun getRoutineNamesSuggestingEquipment(
        exerciseId: Long,
        equipmentTypeId: Long,
    ): List<String>

    @Query("SELECT slot FROM plan_assignment WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId")
    suspend fun getSlotForExercise(routineVersionId: Long, exerciseId: Long): Int?

    /**
     * Whether the slot this exercise occupies holds at least one other exercise, i.e.
     * whether swapping it for the alternative of its slot is something the executant can
     * actually do. An alert never proposes an action that is not available.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM plan_assignment other
            INNER JOIN plan_assignment own
                ON own.routine_version_id = other.routine_version_id
                AND own.slot = other.slot
            WHERE own.exercise_id = :exerciseId
              AND other.exercise_id != :exerciseId
        )
        """,
    )
    suspend fun hasSlotAlternative(exerciseId: Long): Boolean
}
