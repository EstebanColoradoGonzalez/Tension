package com.estebancoloradogonzalez.tension.ui.catalog.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.ui.components.movementCategoryName

/**
 * Selector de la categoría de movimiento: Empuje, Jalón o Pierna (HU-46).
 *
 * Sigue el patrón visual de [ProgressionDifficultySelector] —segmentado de tres opciones con
 * área táctil de 48 dp (RNF06)— con dos diferencias que la historia pide:
 *
 * - **Puede no tener selección** ([selected] nulo). Al crear desde el Diccionario o desde el
 *   plan no hay ninguna preseleccionada (CA-46.13): elegir es parte del dato.
 * - **Puede llegar fijado** ([enabled] = `false`). Desde la sesión la categoría es la del día y
 *   no se puede cambiar (CA-46.11); [supportingText] dice por qué.
 *
 * El error se pinta bajo el campo, en el color de error, igual que el resto del formulario.
 */
@Composable
fun MovementCategorySelector(
    selected: MovementCategory?,
    onSelected: (MovementCategory) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    error: String? = null,
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                1.dp,
                if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Row {
                MovementCategory.entries.forEach { category ->
                    CategoryOption(
                        category = category,
                        isSelected = selected == category,
                        enabled = enabled,
                        onSelected = onSelected,
                    )
                }
            }
        }
        val helper = error ?: supportingText
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun RowScope.CategoryOption(
    category: MovementCategory,
    isSelected: Boolean,
    enabled: Boolean,
    onSelected: (MovementCategory) -> Unit,
) {
    val label = movementCategoryName(category)
    val description = stringResource(R.string.movement_category_select_description, label)
    val containerColor = when {
        isSelected && enabled -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.onSurface.copy(alpha = SelectorAlpha.DISABLED_SELECTED)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        isSelected && enabled -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.surface
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = SelectorAlpha.DISABLED)
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = isSelected, enabled = enabled) { onSelected(category) }
            .semantics { contentDescription = description },
        color = containerColor,
    ) {
        Box(
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

private object SelectorAlpha {
    const val DISABLED = 0.38f
    const val DISABLED_SELECTED = 0.6f
}
