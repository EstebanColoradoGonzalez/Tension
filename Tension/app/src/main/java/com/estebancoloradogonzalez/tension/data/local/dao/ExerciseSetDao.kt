package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.domain.model.SetDistributionData
import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData

@Dao
interface ExerciseSetDao {

    /** Series registradas en la sesión. Cero significa que no se entrenó nada. */
    @Query(
        """
        SELECT COUNT(*)
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.session_id = :sessionId
        """,
    )
    suspend fun countSetsInSession(sessionId: Long): Int

    /** Series ya registradas del ejercicio en la sesion. Cero es la frontera del retiro (CA-43.06). */
    @Query("SELECT COUNT(*) FROM exercise_set WHERE session_exercise_id = :sessionExerciseId")
    suspend fun countSetsForSessionExercise(sessionExerciseId: Long): Int

    @Insert
    suspend fun insert(set: ExerciseSetEntity): Long

    @Query(
        """
        SELECT COUNT(*) + 1 FROM exercise_set
        WHERE session_exercise_id = :sessionExerciseId
        """,
    )
    suspend fun getNextSetNumber(sessionExerciseId: Long): Int

    /**
     * Weight of the previous set registered for this session exercise **with the same
     * implement** — level 2 of the prefilled load precedence (see PrefilledLoadRule).
     *
     * Two sets of the same exercise in the same session may carry different implements
     * (HU-39), and their weights are not comparable, so the memory is per pair (CA-40.03).
     */
    @Query(
        """
        SELECT weight_kg
        FROM exercise_set
        WHERE session_exercise_id = :sessionExerciseId
          AND equipment_type_id = :equipmentTypeId
        ORDER BY set_number DESC
        LIMIT 1
        """,
    )
    suspend fun getLastWeightForPairInSessionExercise(
        sessionExerciseId: Long,
        equipmentTypeId: Long,
    ): Double?

    /**
     * Weight of the last set registered for the **pair** in the most recent closed session
     * in which that pair was trained — level 3 of the prefilled load precedence (see
     * PrefilledLoadRule).
     *
     * Matches on se.exercise_id, not on the slot: the memory belongs to the exercise
     * actually executed, so swapping a slot for its alternative resolves the alternative's
     * own history. Deload sessions are excluded because their load is deliberately
     * reduced and must not become the new baseline.
     *
     * The implement filter is applied **inside** the subquery that picks the session, and
     * that placement is the whole point (HU-40). Applied outside, a session in which the
     * exercise was trained only with the cable would still be "the previous session" for
     * the dumbbell pair, and the query would return nothing — which the caller would read
     * as *no history* and would look exactly like the correct behaviour of CA-40.02.
     */
    @Query(
        """
        SELECT es.weight_kg
        FROM exercise_set es
        WHERE es.equipment_type_id = :equipmentTypeId
          AND es.session_exercise_id = (
            SELECT se.id
            FROM session_exercise se
            INNER JOIN session s ON se.session_id = s.id
            INNER JOIN exercise_set es2 ON es2.session_exercise_id = se.id
            WHERE se.exercise_id = :exerciseId
              AND s.id != :currentSessionId
              AND s.status IN ('COMPLETED', 'INCOMPLETE')
              AND s.deload_id IS NULL
              AND es2.equipment_type_id = :equipmentTypeId
            ORDER BY s.date DESC, s.id DESC
            LIMIT 1
        )
        ORDER BY es.set_number DESC
        LIMIT 1
        """,
    )
    suspend fun getLastWeightForPairInPreviousSession(
        exerciseId: Long,
        equipmentTypeId: Long,
        currentSessionId: Long,
    ): Double?

    /**
     * Capture unit of the last set registered for the **pair** (CA-40.03).
     *
     * Deload sessions are intentionally NOT excluded here: the unit reflects the label
     * printed on the machine, which does not change with the microcycle. For the same
     * reason it matches on se.exercise_id and not on the slot: an alternative exercise is
     * a different implement, with its own label.
     *
     * Resolving it per pair follows from that same argument taken one step further: the
     * label belongs to the machine, and two implements of one exercise are two machines —
     * one may be marked in pounds and the other in kilograms.
     */
    @Query(
        """
        SELECT es.capture_unit
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.exercise_id = :exerciseId
          AND es.equipment_type_id = :equipmentTypeId
        ORDER BY es.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastCaptureUnitForPair(exerciseId: Long, equipmentTypeId: Long): String?

    /**
     * Implemento de la última serie registrada del ejercicio — la preselección del
     * selector de equipamiento (CA-39.04).
     *
     * Igual que la unidad de captura, las sesiones de descarga **no** se excluyen: el
     * implemento con el que se entrena no cambia con el microciclo. Y resuelve sobre
     * `se.exercise_id`, no sobre el slot, porque la memoria pertenece al ejercicio
     * efectivamente ejecutado.
     */
    @Query(
        """
        SELECT es.equipment_type_id
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.exercise_id = :exerciseId
        ORDER BY es.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastEquipmentTypeIdForExercise(exerciseId: Long): Long?

    /**
     * Implemento de la última serie de este `session_exercise` — el ejercicio **en esta
     * sesión**, no en su historia entera.
     *
     * Es el primer nivel de precedencia del selector al registrar una serie (CA-41.05):
     * una vez que el ejecutante cambia de implemento, las series siguientes nacen con el
     * suyo y no con el que sugiere el plan. Nulo mientras no haya ninguna serie, que es
     * cuando la sugerencia del plan toma el relevo.
     */
    @Query(
        """
        SELECT es.equipment_type_id
        FROM exercise_set es
        WHERE es.session_exercise_id = :sessionExerciseId
        ORDER BY es.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastEquipmentTypeIdInSessionExercise(sessionExerciseId: Long): Long?

    /**
     * Series ya registradas del ejercicio con ese implemento.
     *
     * Es lo que impide retirarlo de las opciones del ejercicio (CA-39.10): una serie es
     * inmutable tras su creación y no puede quedar apuntando a un equipamiento que el
     * ejercicio dejó de admitir.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.exercise_id = :exerciseId
          AND es.equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun countSetsByExerciseAndEquipment(exerciseId: Long, equipmentTypeId: Long): Int

    /**
     * Average weight of the **pair** in the last non-deload session before the deload was
     * activated — the baseline the 90% restart load is computed from (CA-40.06).
     *
     * Per pair, and with the implement filter inside the subquery that picks the session,
     * for the same reason as [getLastWeightForPairInPreviousSession]: each implement
     * restarts over its own load, and a pair with no history has nothing to reduce.
     */
    @Query(
        """
        SELECT AVG(es.weight_kg)
        FROM exercise_set es
        WHERE es.equipment_type_id = :equipmentTypeId
          AND es.session_exercise_id = (
            SELECT se.id
            FROM session_exercise se
            INNER JOIN session s ON se.session_id = s.id
            INNER JOIN exercise_set es2 ON es2.session_exercise_id = se.id
            WHERE se.exercise_id = :exerciseId
              AND s.deload_id IS NULL
              AND s.date <= :activationDate
              AND s.status IN ('COMPLETED', 'INCOMPLETE')
              AND es2.equipment_type_id = :equipmentTypeId
            ORDER BY s.date DESC, s.id DESC
            LIMIT 1
        )
        """,
    )
    suspend fun getPreDeloadAvgWeightForPair(
        exerciseId: Long,
        equipmentTypeId: Long,
        activationDate: String,
    ): Double?

    @Query(
        """
        SELECT es.weight_kg AS weightKg, es.reps, es.rir, es.capture_unit AS captureUnit,
               es.equipment_type_id AS equipmentTypeId, et.name AS equipmentTypeName
        FROM exercise_set es
        INNER JOIN equipment_type et ON es.equipment_type_id = et.id
        WHERE es.session_exercise_id = :sessionExerciseId
        ORDER BY es.set_number
        """,
    )
    suspend fun getSetsForSessionExercise(sessionExerciseId: Long): List<ExerciseSetData>

    /**
     * Implements actually used in this session exercise, in catalog order.
     *
     * The decision engine iterates these to evaluate one pair at a time (CA-40.02): a
     * session with dumbbell and cable sets of the same exercise produces two evaluations,
     * consolidated afterwards into the exercise's reading.
     */
    @Query(
        """
        SELECT DISTINCT es.equipment_type_id
        FROM exercise_set es
        WHERE es.session_exercise_id = :sessionExerciseId
        ORDER BY es.equipment_type_id ASC
        """,
    )
    suspend fun getEquipmentIdsInSessionExercise(sessionExerciseId: Long): List<Long>

    /**
     * Sets of the **pair** in the most recent closed session in which that pair was
     * trained — the term the engine compares the current session against (CA-40.02).
     *
     * As in [getLastWeightForPairInPreviousSession], the implement filter goes inside the
     * subquery that picks the session. Outside it, breaking in a new implement would leave
     * the previously used one permanently without a comparison term, and the engine would
     * classify it as *no history* forever — a failure that looks like correct behaviour.
     */
    @Query(
        """
        SELECT es.weight_kg AS weightKg, es.reps, es.rir, es.capture_unit AS captureUnit,
               es.equipment_type_id AS equipmentTypeId, et.name AS equipmentTypeName
        FROM exercise_set es
        INNER JOIN equipment_type et ON es.equipment_type_id = et.id
        WHERE es.equipment_type_id = :equipmentTypeId
          AND es.session_exercise_id = (
            SELECT se2.id
            FROM session_exercise se2
            INNER JOIN session s2 ON se2.session_id = s2.id
            INNER JOIN exercise_set es3 ON es3.session_exercise_id = se2.id
            WHERE se2.exercise_id = :exerciseId
              AND s2.id != :currentSessionId
              AND s2.status IN ('COMPLETED', 'INCOMPLETE')
              AND s2.deload_id IS NULL
              AND es3.equipment_type_id = :equipmentTypeId
            ORDER BY s2.date DESC, s2.id DESC
            LIMIT 1
        )
        ORDER BY es.set_number
        """,
    )
    suspend fun getLastHistoricalSetsForPair(
        exerciseId: Long,
        equipmentTypeId: Long,
        currentSessionId: Long,
    ): List<ExerciseSetData>

    /**
     * Tonelaje por grupo muscular de las sesiones dadas.
     *
     * **Cuentan todas las zonas del ejercicio, principales y secundarias, sin ponderación**
     * (CA-41.04). El `INNER JOIN` sin filtro por `emz.is_primary` no es un olvido: la
     * jerarquía que HU-41 introdujo es informativa para el ejecutante, no un peso de
     * cálculo, y por eso ningún KPI cambió de definición al ganarla.
     *
     * La consecuencia es deliberada: una serie de un ejercicio de tres zonas aporta su
     * tonelaje **íntegro** a cada uno de los tres grupos. Es la misma agregación de antes
     * de HU-41, solo que ahora las zonas son más finas — el eje sigue siendo los 14 grupos.
     *
     * Si alguna vez se quisiera ponderar por jerarquía, este es el sitio, y sería un
     * cambio de definición de KPI que exige historia propia.
     */
    @Query(
        """
        SELECT es.weight_kg AS weightKg, es.reps, mz.muscle_group AS muscleGroup
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE se.session_id IN (:sessionIds)
        """,
    )
    suspend fun getTonnageDataBySessionIds(sessionIds: List<Long>): List<SetTonnageData>

    @Query(
        """
        SELECT es.rir FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.session_id IN (:sessionIds)
        """,
    )
    suspend fun getRirValuesBySessionIds(sessionIds: List<Long>): List<Int>

    /**
     * Average weight of the **pair** in one session — the term the load-velocity KPI
     * compares between the first and the last session of the window (CA-40.07).
     *
     * Per pair because the KPI compares weights: averaging a cable and a dumbbell together
     * would produce a slope out of two magnitudes that are not comparable.
     */
    @Query(
        """
        SELECT AVG(es.weight_kg)
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        WHERE se.exercise_id = :exerciseId
          AND se.session_id = :sessionId
          AND es.equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun getAvgWeightForPairInSession(
        exerciseId: Long,
        equipmentTypeId: Long,
        sessionId: Long,
    ): Double?

    /**
     * Series por zona muscular de las sesiones dadas, base del volumen por grupo (`G2-T1`).
     *
     * Misma invariante que [getTonnageDataBySessionIds]: **todas las zonas cuentan, sin
     * ponderación** (CA-41.04). Sin filtro por `emz.is_primary`, a propósito.
     */
    @Query(
        """
        SELECT mz.name AS muscleZoneName, mz.muscle_group AS muscleGroup, COUNT(*) AS setCount
        FROM exercise_set es
        INNER JOIN session_exercise se ON es.session_exercise_id = se.id
        INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
        INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
        WHERE se.session_id IN (:sessionIds)
        GROUP BY mz.name, mz.muscle_group
        """,
    )
    suspend fun getSetDistributionBySessionIds(sessionIds: List<Long>): List<SetDistributionData>
}

data class ExerciseSetData(
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val captureUnit: String,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
)
