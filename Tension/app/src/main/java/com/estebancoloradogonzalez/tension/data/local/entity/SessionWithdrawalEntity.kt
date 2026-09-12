package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Retiro de un ejercicio **del plan** en una sesión concreta (HU-43).
 *
 * El ejercicio retirado tiene 0 series por CA-43.06, así que su fila de `session_exercise`
 * **se borra**: eso lo saca de golpe del cierre, de la clasificación, del tonelaje y del
 * historial, que es literalmente lo que CA-43.04 pide, y sin tocar ninguna de esas
 * consultas. Un borrado lógico habría obligado a filtrar las diez consultas que hoy leen
 * `session_exercise`, y olvidar una deja un ejercicio retirado contando en silencio.
 *
 * Pero el retiro no puede olvidarse, por dos razones distintas:
 *
 * 1. La invariante de CA-43.05 —*el número de añadidos nunca es menor que el de
 *    retirados*— necesita el conteo de retirados en todo momento.
 * 2. El detalle de la sesión pasada nombra lo que se retiró (CA-43.08), y un conteo no
 *    tiene nombre.
 *
 * Solo se escribe al retirar un ejercicio que trajo el plan. Quitar un añadido es
 * *deshacer*, no retirar: baja el conteo de añadidos y no deja fila aquí. De ahí que el
 * índice único se sostenga — un ejercicio del plan solo puede retirarse una vez por sesión,
 * porque si vuelve, vuelve como añadido.
 */
@Entity(
    tableName = "session_withdrawal",
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
data class SessionWithdrawalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,
)
