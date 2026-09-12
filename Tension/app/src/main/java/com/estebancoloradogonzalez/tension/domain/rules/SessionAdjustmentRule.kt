package com.estebancoloradogonzalez.tension.domain.rules

/**
 * Por qué un ejercicio puede o no retirarse de la sesión en curso (HU-43).
 *
 * Es un veredicto y no un booleano porque el menú contextual muestra **la** causa, no *una*
 * causa: las tres ramas de rechazo son las tres líneas distintas que el ejecutante puede
 * leer bajo la acción deshabilitada.
 */
sealed interface WithdrawalVerdict {

    /** Se puede retirar. */
    data object Allowed : WithdrawalVerdict

    /** Tiene series registradas, y la serie es inmutable (CA-43.06). */
    data class HasSets(val count: Int) : WithdrawalVerdict

    /** No hay presupuesto: hace falta añadir un ejercicio primero (CA-43.04). */
    data object BudgetExhausted : WithdrawalVerdict

    /** Quitarlo dejaría menos añadidos que retirados: hay que reponer antes (CA-43.05). */
    data object WouldBreakInvariant : WithdrawalVerdict
}

/**
 * Las cuatro decisiones del ajuste temporal de la sesión (HU-43).
 *
 * Kotlin puro: sin Android, sin acceso a datos, sin corrutinas. La interfaz la consulta para
 * pintar cada ítem del menú y el repositorio la vuelve a consultar dentro de la transacción
 * — la UI deshabilita, la guarda de datos impide.
 *
 * **El orden de evaluación está fijado aquí, no en quien llama:** primero las series,
 * después el presupuesto. Un ejercicio con series y con presupuesto disponible debe decir
 * que tiene series, porque esa es la causa que no se puede levantar añadiendo nada.
 *
 * **No forma parte del motor de decisión.** No clasifica progresión, no prescribe carga, no
 * dispara alertas: solo decide qué composición admite la sesión de hoy.
 */
object SessionAdjustmentRule {

    /** Prescripción por defecto del añadido, por el enfoque de hipertrofia (CA-43.03). */
    const val DEFAULT_SETS = 3

    const val DEFAULT_REPS = "8-12"

    /** Los isométricos se prescriben en segundos, con la misma regla que el plan. */
    const val ISOMETRIC_REPS = "30-45_SEC"

    /** Los de fallo técnico, sin límite superior. */
    const val TECHNICAL_FAILURE_REPS = "TO_TECHNICAL_FAILURE"

    /**
     * Rango de repeticiones con el que nace el ejercicio añadido.
     *
     * Devuelve valores del mismo vocabulario que `plan_assignment.reps` admite: el añadido
     * nace con un rango del plan, no con uno propio. El modo isométrico manda sobre el de
     * fallo técnico si un ejercicio declarara ambos — un aguante en segundos ya es un fallo
     * técnico, y prescribirlo "sin límite superior" no diría nada.
     */
    fun defaultReps(isIsometric: Boolean, isToTechnicalFailure: Boolean): String = when {
        isIsometric -> ISOMETRIC_REPS
        isToTechnicalFailure -> TECHNICAL_FAILURE_REPS
        else -> DEFAULT_REPS
    }

    /**
     * Cuántos ejercicios del plan pueden retirarse todavía: **uno entra, uno puede salir**
     * (CA-43.04).
     */
    fun budget(addedCount: Int, withdrawnCount: Int): Int = addedCount - withdrawnCount

    /**
     * Veredicto para un ejercicio que **trajo el plan**. Retirarlo consume presupuesto.
     */
    fun verdictForPlanExercise(
        addedCount: Int,
        withdrawnCount: Int,
        completedSets: Int,
    ): WithdrawalVerdict = when {
        completedSets > 0 -> WithdrawalVerdict.HasSets(completedSets)
        budget(addedCount, withdrawnCount) < 1 -> WithdrawalVerdict.BudgetExhausted
        else -> WithdrawalVerdict.Allowed
    }

    /**
     * Veredicto para un ejercicio **añadido**. Quitarlo es deshacer: no genera retiro, pero
     * baja el número de añadidos, y ese número nunca puede quedar por debajo del de
     * retirados (CA-43.05).
     *
     * Cuando el veredicto es [WithdrawalVerdict.WouldBreakInvariant] siempre existe un
     * retiro sin reponer que nombrar en el mensaje, y se puede demostrar: retirar `W`
     * ejercicios del plan exigió `W` añadidos, así que reponiendo `R` de ellos quedan
     * `A + R` añadidos con `A >= W`; quitar uno deja `A + R - 1 >= W + R - 1`, que ya es
     * `>= W` en cuanto `R >= 1`. Luego el veredicto negativo implica `R = 0`.
     */
    fun verdictForAddedExercise(
        addedCount: Int,
        withdrawnCount: Int,
        completedSets: Int,
    ): WithdrawalVerdict = when {
        completedSets > 0 -> WithdrawalVerdict.HasSets(completedSets)
        addedCount - 1 < withdrawnCount -> WithdrawalVerdict.WouldBreakInvariant
        else -> WithdrawalVerdict.Allowed
    }
}
