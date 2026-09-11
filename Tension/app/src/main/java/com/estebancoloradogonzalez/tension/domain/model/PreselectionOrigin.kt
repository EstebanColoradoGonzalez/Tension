package com.estebancoloradogonzalez.tension.domain.model

/**
 * De dónde sale el implemento con el que nace el selector al registrar una serie.
 *
 * Viaja hasta la pantalla porque el rótulo que la acompaña —*sugerido por el plan* frente
 * a *último implemento usado*— es la diferencia entre una preselección que el ejecutante
 * entiende y una que parece arbitraria. Derivarlo en la interfaz obligaría a repetir allí
 * la precedencia de CA-41.05, que es exactamente lo que no debe duplicarse.
 *
 * La precedencia, en orden:
 *
 * 1. [LAST_USED] — el implemento de la última serie **de este ejercicio en esta sesión**.
 *    Es «el último cambio manda»: una vez que el ejecutante elige otro, las series
 *    siguientes nacen con el suyo y no con el del plan.
 * 2. [PLAN_SUGGESTION] — la sugerencia de la asignación, si el ejercicio la tiene y sigue
 *    siendo una opción admitida. Es la primera serie del ejercicio en la sesión.
 * 3. [LAST_USED] otra vez, ahora con el último implemento usado **en cualquier sesión**, y
 *    si nunca se usó, [FIRST_OPTION]. Es CA-39.04 sin cambios: el camino del ejercicio sin
 *    asignación de plan, el que se añade dentro de la sesión.
 */
enum class PreselectionOrigin {
    /**
     * Último implemento con el que se registró una serie de este ejercicio, en esta sesión
     * o en cualquier otra. Rótulo: *último implemento usado*.
     */
    LAST_USED,

    /** Sugerencia del plan para este puesto (CA-41.05). Rótulo: *sugerido por el plan*. */
    PLAN_SUGGESTION,

    /**
     * Primera opción que el ejercicio admite. Ni hay historial ni hay sugerencia aplicable
     * (CA-39.04). **Sin rótulo**: no hay nada que explicar, y un aviso que dijera «primera
     * opción del catálogo» sería ruido sobre el estado más común de un ejercicio nuevo.
     */
    FIRST_OPTION,
}
