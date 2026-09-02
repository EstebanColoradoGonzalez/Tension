package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.DailyRoutineOverride

/**
 * Resuelve qué rutina corresponde hoy: la relación permanente del día, o la reasignación
 * temporal si hay una vigente (HU-36).
 *
 * Es la frontera entre las tres piezas que la historia obliga a separar. La regla decide
 * **qué rutina**; el estado de rotación decide **cuándo cierra el microciclo** y no aparece
 * aquí. Nada de lo que esta regla devuelve alcanza a `rotation_state`, y ese es el modo en
 * que la rotación cíclica queda intacta por construcción.
 *
 * | Entrada | Salida |
 * |---|---|
 * | reasignación de hoy | rutina reasignada, `isOverridden = true` |
 * | reasignación de otra fecha | rutina permanente, `isOverridden = false` |
 * | sin reasignación, permanente nula | `null` — día de descanso |
 * | reasignación de hoy, permanente nula | rutina reasignada |
 * | reasignación de hoy igual a la permanente | la misma rutina, ruta idéntica |
 *
 * La regla no compara: sustituye. Reasignar a la rutina que ya correspondía produce el mismo
 * identificador por la misma ruta, y el único efecto observable es el aviso de que la
 * reasignación aplica solo hoy.
 */
object DailyRoutineRule {

    /**
     * @param routineId la rutina que debe ejecutarse, o nulo si el día no tiene ninguna.
     * @param isOverridden true si [routineId] proviene de una reasignación temporal vigente.
     */
    data class Resolution(
        val routineId: Long?,
        val isOverridden: Boolean,
    )

    /**
     * @param today fecha de hoy en ISO `YYYY-MM-DD`.
     * @param permanentRoutineId rutina que el día tiene asignada de forma permanente.
     * @param override reasignación persistida, de cualquier fecha.
     */
    fun resolve(
        today: String,
        permanentRoutineId: Long?,
        override: DailyRoutineOverride?,
    ): Resolution {
        return if (override != null && override.date == today) {
            Resolution(routineId = override.routineId, isOverridden = true)
        } else {
            Resolution(routineId = permanentRoutineId, isOverridden = false)
        }
    }
}
