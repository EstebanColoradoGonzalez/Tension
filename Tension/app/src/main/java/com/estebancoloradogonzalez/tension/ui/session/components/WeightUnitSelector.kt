package com.estebancoloradogonzalez.tension.ui.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit

/**
 * Segmented two-option selector for the capture unit of the load.
 *
 * Follows the visual and accessibility pattern of the RIR selector: minimum touch
 * target of 48 dp per option (RNF06) and an explicit content description.
 */
@Composable
fun WeightUnitSelector(
    selectedUnit: WeightUnit,
    onUnitSelected: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row {
            UnitOption(
                unit = WeightUnit.KG,
                label = stringResource(R.string.register_set_unit_kg),
                description = stringResource(R.string.register_set_unit_kg_description),
                isSelected = selectedUnit == WeightUnit.KG,
                onUnitSelected = onUnitSelected,
            )
            UnitOption(
                unit = WeightUnit.LB,
                label = stringResource(R.string.register_set_unit_lb),
                description = stringResource(R.string.register_set_unit_lb_description),
                isSelected = selectedUnit == WeightUnit.LB,
                onUnitSelected = onUnitSelected,
            )
        }
    }
}

@Composable
private fun UnitOption(
    unit: WeightUnit,
    label: String,
    description: String,
    isSelected: Boolean,
    onUnitSelected: (WeightUnit) -> Unit,
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp)
            .selectable(selected = isSelected) { onUnitSelected(unit) }
            .semantics { contentDescription = description },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Box(
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
