package com.estebancoloradogonzalez.tension.domain.model

/**
 * Estado del árbol de entrenamiento, con sus dos dimensiones ortogonales.
 *
 * - [stage] expresa el historial acumulado y nunca retrocede mientras se entrene.
 * - [healthScore] expresa la constancia reciente, de 100 (vivo) a 0 (marchito).
 *
 * [daysSinceLastSession] es nulo cuando no hay ninguna sesión registrada, y es la **única**
 * señal que la interfaz consulta para el caso de partida: ni la tarjeta ni la pantalla vuelven
 * a comparar la etapa contra cero. Sin historial no se muestra conteo de días, porque no hay
 * referencia contra la cual contar.
 */
data class TreeState(
    val stage: TreeGrowthStage,
    val healthScore: Int,
    val daysSinceLastSession: Int?,
) {
    /** Si existe al menos una sesión registrada. */
    val hasHistory: Boolean get() = daysSinceLastSession != null
}
