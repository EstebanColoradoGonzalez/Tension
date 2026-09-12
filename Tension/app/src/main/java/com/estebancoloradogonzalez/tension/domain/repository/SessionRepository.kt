package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.AddableExercise
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
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
    suspend fun registerSet(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit,
        equipmentTypeId: Long,
    )
    suspend fun finalizeExercise(sessionExerciseId: Long)
    suspend fun switchAlternativeInSession(sessionExerciseId: Long, exerciseId: Long)

    /**
     * El Diccionario completo, con los que ya están en la sesión marcados (CA-43.01).
     *
     * No los filtra: la hoja los muestra atenuados y con su causa. Ver [AddableExercise].
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
