package com.estebancoloradogonzalez.tension.domain.rules

import kotlin.math.roundToInt

/**
 * Salud del árbol a partir de los días transcurridos desde el último entrenamiento.
 *
 * El margen de 48 horas reconoce que descansar un día es parte del entrenamiento, no una
 * falta. A partir de ahí la salud desciende en línea recta hasta agotarse.
 *
 * **El corte de 14 días no es arbitrario:** coincide con el umbral de crisis de
 * `ROUTINE_INACTIVITY`, de modo que el árbol termina de marchitarse justo cuando el sistema ya
 * considera crítica la inactividad. Son medidas complementarias y no deben acoplarse: la
 * alerta mide inactividad **por rutina** y el árbol la mide **global**.
 */
object TreeHealthRule {

    /** Hasta este número de días la salud se mantiene intacta. */
    const val FULL_HEALTH_DAYS = 2

    /** A partir de este número de días la salud es cero. */
    const val WITHERED_DAYS = 14

    private const val MAX_HEALTH = 100
    private const val MIN_HEALTH = 0

    /**
     * @param daysSinceLastSession días naturales desde la última sesión registrada, o `null`
     *   si no hay ninguna.
     * @return puntaje entero dentro de 0–100.
     */
    fun calculate(daysSinceLastSession: Int?): Int {
        // Sin historial no se castiga: quien acaba de instalar la app no ha tenido todavía
        // oportunidad de entrenar, y marchitarle el árbol de entrada sería castigar la espera.
        val days = daysSinceLastSession ?: return MAX_HEALTH

        return when {
            days <= FULL_HEALTH_DAYS -> MAX_HEALTH
            days >= WITHERED_DAYS -> MIN_HEALTH
            else -> {
                val span = WITHERED_DAYS - FULL_HEALTH_DAYS
                val raw = MAX_HEALTH.toDouble() * (WITHERED_DAYS - days) / span
                raw.roundToInt().coerceIn(MIN_HEALTH, MAX_HEALTH)
            }
        }
    }
}
