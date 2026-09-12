package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.domain.model.ClassificationCount
import com.estebancoloradogonzalez.tension.domain.model.ClassificationCountByGroup
import com.estebancoloradogonzalez.tension.domain.model.ExercisePairSessionRange
import kotlinx.coroutines.flow.Flow

data class SessionExerciseWithDetails(
    val sessionExerciseId: Long,
    val exerciseId: Long?,
    val exerciseName: String?,
    /** Implementos admitidos del ejercicio, separados por [AGGREGATE_SEPARATOR]. */
    val equipmentTypes: String?,
    val muscleZones: String?,
    val sets: Int,
    val reps: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val prescribedLoadKg: Double?,
    val completedSets: Int,
    val muscleGroup: String?,
    val isFinalized: Int,
    val pendingSelection: Int,
    val slot: Int,
    val alternativesInSlot: Int,
    /** Si lo añadió el ejecutante en lugar de traerlo el plan (HU-43). */
    val isExtra: Int,
)

data class SetExerciseInfo(
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val totalSets: Int,
    val reps: String,
    val deloadId: Long?,
)

data class SessionExerciseForProgression(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val progressionDifficulty: String,
    val muscleGroup: String?,
)

data class ExerciseSummaryDto(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    /**
     * Consolidated classification of the exercise in this session (CA-40.04). The
     * classification of each implement travels separately, in `SessionPairSummaryDto`.
     */
    val classification: String?,
    val isBodyweight: Int,
    val isIsometric: Int,
    val avgWeightKg: Double,
    val totalReps: Int,
    val setCount: Int,
    val prescribedSets: Int,
    val isMastered: Int,
    val muscleGroup: String?,
    val previousTotalReps: Int?,
    val isDeload: Int,
)

data class SessionDetailExerciseDto(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val classification: String?,
    val setCount: Int,
    val isDeload: Boolean,
    /** Añadido a aquella sesión, no traído por el plan (HU-43). */
    val isExtra: Boolean,
)

data class ExerciseHistoryEntryDto(
    val date: String,
    val routineName: String,
    val versionNumber: Int,
    val equipmentTypeName: String,
    val avgWeightKg: Double,
    val totalReps: Int,
    val avgRir: Double,
    val classification: String?,
    val isDeload: Boolean,
)

@Dao
interface SessionExerciseDao {

    @Insert
    suspend fun insertAll(exercises: List<SessionExerciseEntity>)

    @Query("SELECT DISTINCT exercise_id FROM session_exercise WHERE session_id = :sessionId")
    suspend fun getExerciseIdsBySessionId(sessionId: Long): List<Long>

    @Query("SELECT * FROM session_exercise WHERE session_id = :sessionId")
    fun getBySessionId(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM session_exercise WHERE id = :sessionExerciseId")
    suspend fun getById(sessionExerciseId: Long): SessionExerciseEntity?

    /** Cuántos ejercicios añadió el ejecutante. Es el término izquierdo de la invariante. */
    @Query("SELECT COUNT(*) FROM session_exercise WHERE session_id = :sessionId AND is_extra = 1")
    suspend fun countExtrasInSession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM session_exercise WHERE session_id = :sessionId AND is_extra = 1")
    fun observeExtraCountInSession(sessionId: Long): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM session_exercise
            WHERE session_id = :sessionId AND exercise_id = :exerciseId
        )
        """,
    )
    suspend fun existsInSession(sessionId: Long, exerciseId: Long): Boolean

    /**
     * Retira el ejercicio de la sesión.
     *
     * Solo se invoca sobre filas con **0 series** (CA-43.06), de modo que el `CASCADE` de
     * `exercise_set` nunca llega a borrar nada: la serie es inmutable y esa inmutabilidad es
     * justo la razón de la restricción, no una consecuencia de ella.
     */
    @Query("DELETE FROM session_exercise WHERE id = :sessionExerciseId")
    suspend fun deleteById(sessionExerciseId: Long)

    @Insert
    suspend fun insert(exercise: SessionExerciseEntity): Long

    /**
     * Exercises of the active session, as the session screen lists them.
     *
     * `prescribedLoadKg` is resolved with `MAX` over the exercise's pairs instead of a join
     * (HU-40): since `exercise_progression` is keyed by the pair, joining it would produce
     * one row per implement and the `GROUP BY se.id` would collapse them by picking an
     * arbitrary one. The value is **informative** — the highest target across implements,
     * consistent with the disjunction of CA-40.04 — because this screen does not know which
     * implement the next set will use. The load that actually governs the prefilled field
     * is the pair's, and it is resolved in `getRegisterSetInfo`.
     *
     * Desde HU-43 todo lo que depende de `slot` va guardado por `is_extra = 0`, y no es una
     * precaución retórica: los puestos del plan empiezan en `0`, que es también el valor por
     * defecto de la columna, así que sin las guardas un ejercicio añadido heredaría por el
     * `LEFT JOIN` las series y repeticiones del puesto 0 y por la subconsulta sus
     * alternativas — las dos cosas que CA-43.01 y CA-43.03 prohíben, y ninguna de las dos
     * lanza excepción. `prescribed_sets` y `prescribed_reps` van delante del plan en el
     * `COALESCE`: quien tiene prescripción propia no pide prestada la de nadie.
     *
     * El orden antepone `is_extra` porque el añadido entra **al final** de la lista, fuera
     * de la estructura de puestos (CA-43.01), y `se.id` desempata los añadidos por orden de
     * llegada.
     */
    @Query(
        """
        SELECT
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = se.exercise_id
                ORDER BY et.id
            )) AS equipmentTypes,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = se.exercise_id
                ORDER BY emz.is_primary DESC, mz.sort_order
            )) AS muscleZones,
            COALESCE(se.prescribed_sets, pa.sets, 4) AS sets,
            COALESCE(se.prescribed_reps, pa.reps, '8-12') AS reps,
            COALESCE(e.is_bodyweight, 0) AS isBodyweight,
            COALESCE(e.is_isometric, 0) AS isIsometric,
            COALESCE(e.is_to_technical_failure, 0) AS isToTechnicalFailure,
            (SELECT MAX(ep.prescribed_load_kg) FROM exercise_progression ep
             WHERE ep.exercise_id = se.exercise_id) AS prescribedLoadKg,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = se.exercise_id
             ORDER BY emz2.is_primary DESC, mz2.sort_order ASC
             LIMIT 1) AS muscleGroup,
            se.is_finalized AS isFinalized,
            se.pending_selection AS pendingSelection,
            se.slot AS slot,
            CASE WHEN se.is_extra = 1 THEN 0 ELSE (
                SELECT COUNT(*) FROM plan_assignment pa_alt
                WHERE pa_alt.routine_version_id = s.routine_version_id
                  AND pa_alt.slot = se.slot
            ) END AS alternativesInSlot,
            se.is_extra AS isExtra
        FROM session_exercise se
        LEFT JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN session s ON se.session_id = s.id
        LEFT JOIN plan_assignment pa ON pa.routine_version_id = s.routine_version_id
            AND se.is_extra = 0
            AND pa.exercise_id = CASE
                WHEN se.exercise_id IS NOT NULL
                    THEN se.exercise_id
                ELSE
                    (SELECT pa2.exercise_id FROM plan_assignment pa2
                     WHERE pa2.routine_version_id = s.routine_version_id
                       AND pa2.slot = se.slot
                     ORDER BY pa2.sort_order ASC
                     LIMIT 1)
            END
        WHERE se.session_id = :sessionId
        GROUP BY se.id
        ORDER BY se.is_extra ASC, se.slot ASC, se.id ASC
        """,
    )
    fun getBySessionIdWithDetails(sessionId: Long): Flow<List<SessionExerciseWithDetails>>

    @Query(
        """
        SELECT
            s.id AS sessionId,
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            COALESCE(se.prescribed_sets, pa.sets, 4) AS totalSets,
            COALESCE(se.prescribed_reps, pa.reps, '8-12') AS reps,
            s.deload_id AS deloadId
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN session s ON se.session_id = s.id
        LEFT JOIN plan_assignment pa ON pa.routine_version_id = s.routine_version_id
            AND se.is_extra = 0
            AND pa.exercise_id = se.exercise_id
        WHERE se.id = :sessionExerciseId
        """,
    )
    suspend fun getExerciseInfoForSet(sessionExerciseId: Long): SetExerciseInfo?

    @Query(
        """
        SELECT 
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            (SELECT e2.name FROM exercise e2
             WHERE e2.id = se.exercise_id) AS exerciseName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.progression_difficulty AS progressionDifficulty,
            (SELECT mz.muscle_group FROM exercise_muscle_zone emz
             INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
             WHERE emz.exercise_id = se.exercise_id
             ORDER BY emz.is_primary DESC, mz.sort_order ASC
             LIMIT 1) AS muscleGroup
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        WHERE se.session_id = :sessionId AND se.exercise_id IS NOT NULL
        """,
    )
    suspend fun getSessionExercisesForProgression(sessionId: Long): List<SessionExerciseForProgression>

    @Query(
        """
        SELECT mz.muscle_group
        FROM exercise_muscle_zone emz
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE emz.exercise_id = :exerciseId
        ORDER BY emz.is_primary DESC, mz.sort_order ASC
        LIMIT 1
        """,
    )
    /**
     * Grupo de agregación del ejercicio, del que cuelga la alerta `TONNAGE_DROP`.
     *
     * El nombre prometía «principal» desde antes de que la jerarquía existiera: hasta
     * HU-41 el `LIMIT 1` no tenía `ORDER BY` y devolvía un grupo arbitrario, correcto solo
     * porque casi todo ejercicio tenía una zona. Con la jerarquía el nombre pasa a ser
     * cierto: primero las principales, y entre ellas la primera del catálogo.
     */
    suspend fun getPrimaryMuscleGroupByExercise(exerciseId: Long): String?

    @Query(
        """
        UPDATE session_exercise
        SET progression_classification = :classification
        WHERE id = :sessionExerciseId
        """,
    )
    suspend fun updateProgressionClassification(sessionExerciseId: Long, classification: String?)

    @Query(
        """
        UPDATE session_exercise
        SET is_finalized = 1
        WHERE id = :sessionExerciseId
        """,
    )
    suspend fun finalizeExercise(sessionExerciseId: Long)

    @Query(
        """
        UPDATE session_exercise
        SET is_finalized = 1
        WHERE session_id = :sessionId AND is_finalized = 0
        """,
    )
    suspend fun finalizeAllInSession(sessionId: Long)

    @Query(
        """
        UPDATE session_exercise
        SET exercise_id = :exerciseId
        WHERE id = :sessionExerciseId
        """,
    )
    suspend fun switchAlternativeExercise(sessionExerciseId: Long, exerciseId: Long)

    /**
     * One row per exercise of the session, for the post-session summary.
     *
     * Since HU-40 it no longer reads `exercise_progression`: the prescribed load belongs to
     * the pair and travels in `SessionPairSummaryDto`, one row per implement. What is kept
     * here is `isMastered`, resolved as *some pair is mastered* — mastery is a property of
     * the isometric exercise, which has no external load and in practice a single pair.
     */
    @Query(
        """
        SELECT
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            se.progression_classification AS classification,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            COALESCE(
                (SELECT AVG(es.weight_kg) FROM exercise_set es WHERE es.session_exercise_id = se.id),
                0.0
            ) AS avgWeightKg,
            COALESCE(
                (SELECT SUM(es.reps) FROM exercise_set es WHERE es.session_exercise_id = se.id),
                0
            ) AS totalReps,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS setCount,
            COALESCE(
                se.prescribed_sets,
                (SELECT pa.sets FROM plan_assignment pa
                 WHERE pa.routine_version_id = s.routine_version_id
                   AND pa.exercise_id = se.exercise_id),
                4
            ) AS prescribedSets,
            CASE WHEN EXISTS (
                SELECT 1 FROM exercise_progression ep
                WHERE ep.exercise_id = se.exercise_id AND ep.status = 'MASTERED'
            ) THEN 1 ELSE 0 END AS isMastered,
            (SELECT mz.muscle_group FROM exercise_muscle_zone emz
             INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
             WHERE emz.exercise_id = se.exercise_id
             ORDER BY emz.is_primary DESC, mz.sort_order ASC
             LIMIT 1) AS muscleGroup,
            (SELECT SUM(es3.reps)
             FROM exercise_set es3
             WHERE es3.session_exercise_id = (
                 SELECT se3.id
                 FROM session_exercise se3
                 INNER JOIN session s3 ON se3.session_id = s3.id
                 WHERE se3.exercise_id = se.exercise_id
                   AND s3.id != :sessionId
                   AND s3.status IN ('COMPLETED', 'INCOMPLETE')
                   AND s3.deload_id IS NULL
                 ORDER BY s3.date DESC, s3.id DESC
                 LIMIT 1
             )
            ) AS previousTotalReps,
            CASE WHEN s.deload_id IS NOT NULL THEN 1 ELSE 0 END AS isDeload
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN session s ON se.session_id = s.id
        WHERE se.session_id = :sessionId
        GROUP BY se.id
        HAVING setCount > 0
        ORDER BY se.is_extra ASC, se.slot ASC, COALESCE(
          (SELECT pa2.sort_order FROM plan_assignment pa2
           WHERE pa2.routine_version_id = s.routine_version_id
           AND pa2.exercise_id = se.exercise_id),
          9999
        ) ASC
        """,
    )
    suspend fun getExercisesForSummary(sessionId: Long): List<ExerciseSummaryDto>

    @Query(
        """
        SELECT
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            e.is_bodyweight AS isBodyweight,
            e.progression_difficulty AS progressionDifficulty,
            SUM(CASE WHEN se.progression_classification = 'POSITIVE_PROGRESSION' THEN 1 ELSE 0 END) AS positiveCount,
            COUNT(se.progression_classification) AS totalCount
        FROM session_exercise se
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN exercise e ON se.exercise_id = e.id
        WHERE s.status IN ('COMPLETED', 'INCOMPLETE')
          AND s.deload_id IS NULL
          AND s.date >= :startDate
          AND se.progression_classification IS NOT NULL
        GROUP BY se.exercise_id
        """,
    )
    suspend fun getClassificationCountsByPeriod(startDate: String): List<ClassificationCount>

    @Query(
        """
        SELECT
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            e.is_bodyweight AS isBodyweight,
            e.progression_difficulty AS progressionDifficulty,
            SUM(CASE WHEN se.progression_classification = 'POSITIVE_PROGRESSION' THEN 1 ELSE 0 END) AS positiveCount,
            COUNT(se.progression_classification) AS totalCount
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        WHERE se.session_id IN (:sessionIds)
          AND se.progression_classification IS NOT NULL
        GROUP BY se.exercise_id
        """,
    )
    suspend fun getClassificationCountsForSessions(sessionIds: List<Long>): List<ClassificationCount>

    /**
     * Session range of every **(exercise, implement) pair** trained in the window.
     *
     * The load-velocity KPI compares weights between sessions, so it is computed over each
     * pair and consolidated afterwards (CA-40.07). Grouping by exercise alone would take
     * the first session of the dumbbell and the last of the cable as the two ends of one
     * slope, which is a number made of two incomparable magnitudes.
     */
    @Query(
        """
        SELECT
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            es.equipment_type_id AS equipmentTypeId,
            et.name AS equipmentTypeName,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            MIN(se.session_id) AS firstSessionId,
            MAX(se.session_id) AS lastSessionId,
            COUNT(DISTINCT se.session_id) AS sessionCount
        FROM session_exercise se
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN exercise_set es ON es.session_exercise_id = se.id
        INNER JOIN equipment_type et ON es.equipment_type_id = et.id
        WHERE s.status IN ('COMPLETED', 'INCOMPLETE')
          AND s.deload_id IS NULL
          AND s.date >= :startDate
        GROUP BY se.exercise_id, es.equipment_type_id
        """,
    )
    suspend fun getPairSessionRangeByPeriod(startDate: String): List<ExercisePairSessionRange>

    /**
     * Clasificaciones por grupo muscular, base de la tendencia de progresión (`G3-T1`).
     *
     * **Todas las zonas del ejercicio cuentan, sin ponderación** (CA-41.04): el ejercicio
     * aporta su clasificación a cada grupo que toca. La jerarquía de HU-41 no filtra aquí,
     * igual que no filtra en el tonelaje ni en el volumen.
     */
    @Query(
        """
        SELECT
            mz.muscle_group AS muscleGroup,
            SUM(CASE WHEN se.progression_classification = 'POSITIVE_PROGRESSION' THEN 1 ELSE 0 END) AS positiveCount,
            COUNT(se.progression_classification) AS totalCount
        FROM session_exercise se
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE se.session_id IN (:sessionIds)
          AND s.deload_id IS NULL
          AND se.progression_classification IS NOT NULL
        GROUP BY mz.muscle_group
        """,
    )
    suspend fun getClassificationCountsBySessionIds(
        sessionIds: List<Long>,
    ): List<ClassificationCountByGroup>

    @Query(
        """
        SELECT
            se.id AS sessionExerciseId,
            se.exercise_id AS exerciseId,
            e.name AS exerciseName,
            se.progression_classification AS classification,
            (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS setCount,
            CASE WHEN s.deload_id IS NOT NULL THEN 1 ELSE 0 END AS isDeload,
            se.is_extra AS isExtra
        FROM session_exercise se
        INNER JOIN exercise e ON se.exercise_id = e.id
        INNER JOIN session s ON se.session_id = s.id
        WHERE se.session_id = :sessionId
        GROUP BY se.id
        HAVING setCount > 0
        ORDER BY se.is_extra ASC, se.slot ASC, COALESCE(
          (SELECT pa2.sort_order FROM plan_assignment pa2
           WHERE pa2.routine_version_id = s.routine_version_id
           AND pa2.exercise_id = se.exercise_id),
          9999
        ) ASC
        """,
    )
    suspend fun getExercisesForSessionDetail(sessionId: Long): List<SessionDetailExerciseDto>

    /**
     * Historial del ejercicio, **una entrada por sesión y por implemento**.
     *
     * La segmentación va en el `GROUP BY` y no en el ViewModel porque los promedios se
     * calculan aquí: agrupar solo por `se.id` mezclaría el peso de la mancuerna con el de
     * la polea antes de que el dato salga de la base, y CA-39.08 exige que una serie de
     * polea nunca se compare con una de mancuerna. Una sesión en la que se usaron dos
     * implementos produce dos entradas con la misma fecha.
     */
    @Query(
        """
        SELECT
            s.date,
            r.name AS routineName,
            rv.version_number AS versionNumber,
            COALESCE(AVG(es.weight_kg), 0.0) AS avgWeightKg,
            COALESCE(SUM(es.reps), 0) AS totalReps,
            COALESCE(AVG(es.rir), 0.0) AS avgRir,
            se.progression_classification AS classification,
            CASE WHEN s.deload_id IS NOT NULL THEN 1 ELSE 0 END AS isDeload,
            et.name AS equipmentTypeName
        FROM session_exercise se
        INNER JOIN session s ON se.session_id = s.id
        INNER JOIN routine_version rv ON s.routine_version_id = rv.id
        INNER JOIN routine r ON rv.routine_id = r.id
        INNER JOIN exercise_set es ON es.session_exercise_id = se.id
        INNER JOIN equipment_type et ON es.equipment_type_id = et.id
        WHERE se.exercise_id = :exerciseId
          AND s.status IN ('COMPLETED', 'INCOMPLETE')
        GROUP BY se.id, es.equipment_type_id
        ORDER BY s.date DESC, s.id DESC, et.id ASC
        """,
    )
    suspend fun getExerciseHistoryEntries(exerciseId: Long): List<ExerciseHistoryEntryDto>
}
