package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.AddableExercise
import com.estebancoloradogonzalez.tension.domain.model.CorrectableSet
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.PrefilledLoad
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionAdjustment
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.model.SessionPreviewExercise
import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getTodaySession(): Flow<TodaySession>
    suspend fun startSession(routineVersionId: Long): Long
    fun getActiveSession(): Flow<ActiveSession?>
    fun getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>>
    fun getRotationState(): Flow<RotationState?>
    fun getSessionRoutineVersion(sessionId: Long): Flow<Pair<String, Int>?>
    suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo?

    /**
     * Peso y unidad con los que precargar el formulario para un par concreto (CA-40.03).
     *
     * Existe aparte de [getRegisterSetInfo] porque el implemento se puede cambiar sin salir
     * de la pantalla, y al cambiarlo la precarga deja de ser válida: pertenecía a otro par.
     */
    suspend fun getPrefilledLoadForPair(
        sessionExerciseId: Long,
        equipmentTypeId: Long,
    ): PrefilledLoad?
    /**
     * Registra la serie y devuelve el aviso en directo que le corresponde (CA-46.05), o
     * `null` si no hay nada que decir. El aviso es informativo: la serie se persiste tal
     * como llegó, haya aviso o no.
     */
    suspend fun registerSet(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit,
        equipmentTypeId: Long,
    ): SetRangeNotice?

    /**
     * The set inside the correction window, if this session exercise owns it (CA-47.01,
     * CA-47.03): the last registered set of the session, and only while the session is in
     * progress. `null` otherwise — another exercise owns it, there are no sets or the
     * session is already closed (CA-47.07, CA-47.08).
     */
    suspend fun getCorrectableSet(sessionExerciseId: Long): CorrectableSet?

    /**
     * Corrects set [setId] with the given values and rebuilds the derived values that
     * store something of it — the pair's 1RM — as if the mistake had never existed
     * (CA-47.04, CA-47.11).
     *
     * The window is re-evaluated **inside** the transaction: if [setId] is no longer the
     * last set of the session in progress of [sessionExerciseId], it throws
     * [IllegalStateException] and writes nothing. Only weight, unit, repetitions and RIR
     * change; the implement, the set number and the session stay fixed.
     */
    suspend fun correctSet(
        sessionExerciseId: Long,
        setId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit,
    )

    /**
     * Categoría del día: la de la rutina de la sesión (CA-46.09). `null` solo si la sesión no
     * existe o su rutina lleva un código no reconocido.
     */
    suspend fun getSessionMovementCategory(sessionId: Long): MovementCategory?
    suspend fun finalizeExercise(sessionExerciseId: Long)
    suspend fun switchAlternativeInSession(sessionExerciseId: Long, exerciseId: Long)

    /**
     * El Diccionario de la categoría del día (CA-46.10), con los que ya están en la sesión
     * marcados (CA-43.01).
     *
     * No filtra los ya presentes: la hoja los muestra atenuados y con su causa. Ver
     * [AddableExercise]. Los de otra categoría sí quedan fuera, también de la búsqueda.
     */
    fun getAddableExercises(sessionId: Long): Flow<List<AddableExercise>>

    /**
     * Añade un ejercicio del Diccionario a la sesión en curso (CA-43.01).
     *
     * Entra al final, **sin puesto** y por tanto sin alternativas intercambiables, con la
     * prescripción por defecto de CA-43.03. El plan no se toca. Devuelve el id de la fila
     * de `session_exercise` creada.
     */
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long): Long

    /**
     * Retira un ejercicio de la sesión en curso (CA-43.04, CA-43.05, CA-43.06).
     *
     * Lanza si el veredicto de `SessionAdjustmentRule` no es favorable. La interfaz ya
     * deshabilita la acción con su causa; esta guarda cubre la ruta de datos.
     */
    suspend fun withdrawFromSession(sessionExerciseId: Long)

    /** Añadidos, retirados y retiros sin reponer de la sesión en curso (HU-43). */
    fun getSessionAdjustment(sessionId: Long): Flow<SessionAdjustment>
    suspend fun closeSession(sessionId: Long)
    suspend fun getSessionSummaryData(sessionId: Long): SessionSummaryData
    suspend fun activateDeload()
    suspend fun getDeloadState(): DeloadState
    suspend fun getDeloadIdBySessionId(sessionId: Long): Long?
    suspend fun getRoutineVersionIdBySessionId(sessionId: Long): Long
    suspend fun getSessionHistory(): List<SessionHistoryItem>
    suspend fun getSessionDetail(sessionId: Long): SessionDetail
    suspend fun getExerciseHistory(exerciseId: Long): ExerciseHistoryData
    fun getSessionPreviewExercises(routineVersionId: Long): Flow<List<SessionPreviewExercise>>
    suspend fun hasActiveDeload(): Boolean
    /**
     * Declara que hoy no se entrena. Resuelve el día sin dejar sesión alguna: si había una en
     * curso sin series, se descarta.
     */
    suspend fun skipToday()

    /** Si la sesión en curso, de haberla, ya tiene alguna serie registrada. */
    suspend fun hasSetsInActiveSession(): Boolean

    /** La sesión en curso que quedó de un día anterior, si la hay. */
    suspend fun getStaleActiveSessionId(): Long?

    /** Borra la sesión sin registrarla. Solo para sesiones sin ninguna serie. */
    suspend fun discardSession(sessionId: Long)

    /** Revierte la omisión de hoy y devuelve el día a su propuesta. */
    suspend fun undoSkipToday()

    suspend fun hasActiveSession(): Boolean
    suspend fun hasActiveSessionForVersion(routineVersionId: Long): Boolean
}
