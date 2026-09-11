package com.estebancoloradogonzalez.tension.ui.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone

private val ROW_MIN_HEIGHT = 48.dp
private val LIST_MAX_HEIGHT = 380.dp

/**
 * Selector de una zona muscular entre las 33 del catálogo, agrupadas por sus 14 grupos
 * (CA-41.01, CA-41.02).
 *
 * **Diálogo y no lista embebida**, a diferencia de [EquipmentMultiSelector]: 15 casillas
 * caben en el formulario, 33 filas con encabezados de grupo no. El coste asumido es un
 * toque más por zona; a cambio el formulario sigue siendo legible de un vistazo y la lista
 * puede permitirse buscador.
 *
 * [zones] llega **ya ordenada** por el catálogo —los grupos en orden anatómico y las zonas
 * de mayor a menor dentro de cada uno—, así que aquí no se ordena nada: se agrupa
 * conservando el orden de llegada. Ordenar por nombre daría *Pectoral Inferior, Mayor,
 * Medio, Superior*, que no significa nada.
 *
 * [alreadyChosenHere] son las zonas que ya están en el campo que abrió el diálogo, y
 * [chosenInOtherField] las del otro. Las segundas **no se pueden elegir**: una zona no
 * puede ser principal y secundaria del mismo ejercicio (CA-41.09). Se muestran igualmente,
 * marcadas y apagadas, porque esconderlas dejaría al ejecutante buscando una zona que
 * parece no existir.
 */
@Composable
fun MuscleZonePickerDialog(
    zones: List<MuscleZone>,
    alreadyChosenHere: Set<Long>,
    chosenInOtherField: Set<Long>,
    isPrimaryField: Boolean,
    onZoneSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val visible = remember(zones, query) {
        if (query.isBlank()) {
            zones
        } else {
            zones.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isPrimaryField) {
                        R.string.muscle_zone_picker_title_primary
                    } else {
                        R.string.muscle_zone_picker_title_secondary
                    },
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.muscle_zone_picker_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = LIST_MAX_HEIGHT)
                        .padding(top = 8.dp),
                ) {
                    // El encabezado se emite junto a la primera zona de su grupo en vez de
                    // con `stickyHeader`: con el buscador activo un grupo puede quedarse
                    // sin zonas, y entonces su encabezado no debe aparecer.
                    itemsIndexed(visible, key = { _, zone -> zone.id }) { index, zone ->
                        val isFirstOfGroup =
                            index == 0 || visible[index - 1].muscleGroup != zone.muscleGroup
                        if (isFirstOfGroup) {
                            MuscleGroupHeader(zone.muscleGroup)
                        }
                        MuscleZoneRow(
                            zone = zone,
                            isChosenHere = zone.id in alreadyChosenHere,
                            isChosenElsewhere = zone.id in chosenInOtherField,
                            onClick = { onZoneSelected(zone.id) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.muscle_zone_picker_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun MuscleGroupHeader(muscleGroup: String) {
    Text(
        text = muscleGroup.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun MuscleZoneRow(
    zone: MuscleZone,
    isChosenHere: Boolean,
    isChosenElsewhere: Boolean,
    onClick: () -> Unit,
) {
    val selectable = !isChosenHere && !isChosenElsewhere
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .then(if (selectable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = zone.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selectable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        when {
            isChosenHere -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            // El texto y no un punto: RNF05 prohíbe que una señal viaje solo en la forma
            // o el color, y «ya elegida en el otro campo» es exactamente lo que el
            // ejecutante necesita saber para dejar de intentarlo.
            isChosenElsewhere -> Text(
                text = stringResource(R.string.muscle_zone_picker_already_other_field),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
