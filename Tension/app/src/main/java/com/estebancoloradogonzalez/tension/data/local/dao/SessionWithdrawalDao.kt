package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.SessionWithdrawalEntity
import kotlinx.coroutines.flow.Flow

/** Ejercicio retirado de una sesión, con el nombre que el mensaje y el historial necesitan. */
data class WithdrawnExerciseDto(
    val exerciseId: Long,
    val exerciseName: String,
)

@Dao
interface SessionWithdrawalDao {

    @Insert
    suspend fun insert(withdrawal: SessionWithdrawalEntity): Long

    /** Retiros de la sesión, repuestos incluidos. Es el término derecho de la invariante. */
    @Query("SELECT COUNT(*) FROM session_withdrawal WHERE session_id = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM session_withdrawal WHERE session_id = :sessionId")
    fun observeCountBySession(sessionId: Long): Flow<Int>

    /**
     * Retiros **no repuestos**: los que siguen sin estar en la sesión.
     *
     * Reponer no borra el retiro —la aritmética de la invariante depende de que siga
     * contado (CA-43.05)—, pero un ejercicio que acabó estando en la sesión no es algo que
     * el mensaje deba pedir reponer, ni algo que el historial deba anunciar como retirado.
     * El filtro vive en SQL porque es el mismo para los dos usos.
     */
    @Query(
        """
        SELECT sw.exercise_id AS exerciseId, e.name AS exerciseName
        FROM session_withdrawal sw
        INNER JOIN exercise e ON sw.exercise_id = e.id
        WHERE sw.session_id = :sessionId
          AND sw.exercise_id NOT IN (
              SELECT se.exercise_id FROM session_exercise se
              WHERE se.session_id = :sessionId AND se.exercise_id IS NOT NULL
          )
        ORDER BY sw.id ASC
        """,
    )
    fun observePendingBySession(sessionId: Long): Flow<List<WithdrawnExerciseDto>>

    @Query(
        """
        SELECT sw.exercise_id AS exerciseId, e.name AS exerciseName
        FROM session_withdrawal sw
        INNER JOIN exercise e ON sw.exercise_id = e.id
        WHERE sw.session_id = :sessionId
          AND sw.exercise_id NOT IN (
              SELECT se.exercise_id FROM session_exercise se
              WHERE se.session_id = :sessionId AND se.exercise_id IS NOT NULL
          )
        ORDER BY sw.id ASC
        """,
    )
    suspend fun getPendingBySession(sessionId: Long): List<WithdrawnExerciseDto>
}
