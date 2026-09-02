package com.estebancoloradogonzalez.tension.ui.tree

import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.domain.model.TreeState

data class TreeUiState(
    val isLoading: Boolean = true,
    val treeState: TreeState? = null,
) {
    val stage: TreeGrowthStage get() = treeState?.stage ?: TreeGrowthStage.SEED

    val healthScore: Int get() = treeState?.healthScore ?: 0

    /**
     * Sin historial no se muestra conteo de días: no hay referencia contra la cual contar, y
     * mostrar un cero sugeriría una inactividad que no ha ocurrido.
     */
    val hasHistory: Boolean get() = treeState?.hasHistory == true

    val daysSinceLastSession: Int? get() = treeState?.daysSinceLastSession
}
