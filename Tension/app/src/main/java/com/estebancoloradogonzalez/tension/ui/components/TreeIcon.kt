package com.estebancoloradogonzalez.tension.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

/** Bandas de salud que gobiernan tanto el tinte del ícono como los textos. */
private const val HEALTH_HIGH_MIN = 67
private const val HEALTH_MEDIUM_MIN = 34
private const val HEALTH_LOW_MIN = 1

/**
 * El árbol como ícono vectorial teñido.
 *
 * Las dos dimensiones se leen por separado: la **forma** comunica la etapa y el **color**
 * comunica la salud. Por eso son cuatro recursos y no una matriz de veinte — el tinte se
 * resuelve aquí, en tiempo de ejecución, desde el tema.
 *
 * @param hasHistory sin historial el árbol se pinta en gris neutro y no en el verde de salud
 *   100: presentarlo como "sanísimo" cuando todavía no ha germinado sería ruido.
 */
@Composable
fun TreeIcon(
    stage: TreeGrowthStage,
    healthScore: Int,
    hasHistory: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = treeIconRes(stage)),
        contentDescription = stringResource(R.string.tree_icon_description),
        tint = treeHealthColor(healthScore = healthScore, hasHistory = hasHistory),
        modifier = modifier.size(size),
    )
}

@DrawableRes
private fun treeIconRes(stage: TreeGrowthStage): Int = when (stage) {
    TreeGrowthStage.SEED -> R.drawable.ic_tree_seed
    TreeGrowthStage.SPROUT -> R.drawable.ic_tree_sprout
    TreeGrowthStage.YOUNG -> R.drawable.ic_tree_young
    TreeGrowthStage.MATURE -> R.drawable.ic_tree_mature
}

/**
 * Tinte del árbol por banda de salud. Es presentación, no regla de negocio: la capa de dominio
 * no conoce colores.
 */
@Composable
private fun treeHealthColor(healthScore: Int, hasHistory: Boolean): Color {
    val colors = LocalTensionSemanticColors.current
    return when {
        !hasHistory -> colors.treeSeed
        healthScore >= HEALTH_HIGH_MIN -> colors.treeHealthy
        healthScore >= HEALTH_MEDIUM_MIN -> colors.treeDry
        healthScore >= HEALTH_LOW_MIN -> colors.treeWithering
        else -> colors.treeWithered
    }
}

/** Nombre de la etapa, para la pantalla dedicada. */
@StringRes
fun treeStageLabelRes(stage: TreeGrowthStage): Int = when (stage) {
    TreeGrowthStage.SEED -> R.string.tree_stage_seed
    TreeGrowthStage.SPROUT -> R.string.tree_stage_sprout
    TreeGrowthStage.YOUNG -> R.string.tree_stage_young
    TreeGrowthStage.MATURE -> R.string.tree_stage_mature
}

/**
 * Línea de estado de la tarjeta de Inicio. Sin historial no describe salud, porque todavía no
 * hay árbol del que hablar.
 */
@StringRes
fun treeCardMessageRes(healthScore: Int, hasHistory: Boolean): Int = when {
    !hasHistory -> R.string.tree_card_seed
    healthScore >= HEALTH_HIGH_MIN -> R.string.tree_card_high
    healthScore >= HEALTH_MEDIUM_MIN -> R.string.tree_card_medium
    healthScore >= HEALTH_LOW_MIN -> R.string.tree_card_low
    else -> R.string.tree_card_withered
}
