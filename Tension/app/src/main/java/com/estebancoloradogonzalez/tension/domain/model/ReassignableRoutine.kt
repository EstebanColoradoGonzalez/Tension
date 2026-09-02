package com.estebancoloradogonzalez.tension.domain.model

/**
 * Opción del selector de reasignación temporal: una rutina ejecutable hoy.
 *
 * Se ofrece toda rutina cuya versión vigente tenga ejercicios, no solo las que tienen día
 * asignado — una rutina creada por el ejecutante no queda inalcanzable por no tener día.
 * [weekDays] son los días que ejecutan la rutina, vacío cuando ninguno la reclama: una
 * rutina puede ocupar más de un día de la semana.
 *
 * [isTodaysRoutine] marca la que ya correspondía a hoy. No se excluye de la lista:
 * seleccionarla es un caso válido y su comportamiento es idéntico a no reasignar.
 */
data class ReassignableRoutine(
    val routineId: Long,
    val routineName: String,
    val routineVersionId: Long,
    val versionNumber: Int,
    val weekDays: List<WeekDay>,
    val isTodaysRoutine: Boolean,
)
