package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage

/**
 * Etapa de crecimiento del árbol a partir del total de sesiones registradas.
 *
 * El crecimiento es lento y meritorio a propósito: la estatura hace visible el esfuerzo de
 * meses, no el de la última semana.
 *
 * **La etapa no retrocede porque el conteo no baja, no porque se guarde un máximo.** La
 * invariante que exige el criterio es frente a la *salud* —faltar marchita el árbol, no lo
 * encoge—, y al ser función monótona del total de sesiones cerradas queda garantizada sin
 * memoria adicional. Un máximo pegajoso además impediría que restaurar un respaldo dejara el
 * árbol en el estado que corresponde al historial restaurado.
 */
object TreeGrowthStageRule {

    const val SPROUT_MIN = 1
    const val YOUNG_MIN = 10
    const val MATURE_MIN = 30

    /**
     * @param sessionCount total de sesiones registradas (`COMPLETED` e `INCOMPLETE`).
     */
    fun resolve(sessionCount: Int): TreeGrowthStage = when {
        sessionCount >= MATURE_MIN -> TreeGrowthStage.MATURE
        sessionCount >= YOUNG_MIN -> TreeGrowthStage.YOUNG
        sessionCount >= SPROUT_MIN -> TreeGrowthStage.SPROUT
        else -> TreeGrowthStage.SEED
    }
}
