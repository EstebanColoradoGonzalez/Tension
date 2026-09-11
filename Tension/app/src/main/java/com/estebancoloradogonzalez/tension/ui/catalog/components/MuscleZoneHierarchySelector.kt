package com.estebancoloradogonzalez.tension.ui.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone

private val ROW_MIN_HEIGHT = 48.dp
private val FIELD_CORNER = 4.dp

/**
 * Zonas musculares de un ejercicio, con su jerarquía (CA-41.02, CA-41.09).
 *
 * **Dos campos separados** y no una lista única con una marca por zona. La jerarquía queda
 * implícita en el campo donde se elige, sin un control extra por fila, y —lo que decide el
 * diseño— **la obligatoriedad se lee donde se incumple**: el aviso de «al menos una
 * principal» vive en el campo que lo exige. Con 33 zonas, un error al pie del formulario
 * quedaría lejos del sitio donde hay que arreglarlo.
 *
 * Se descartó el buscador con chips por la misma razón: un chip no dice de qué campo es.
 *
 * El campo de principales es obligatorio y lleva asterisco; el de secundarias es opcional
 * y lo dice. Ninguno de los dos puede contener una zona que esté en el otro — el diálogo
 * la ofrece marcada y no seleccionable.
 */
@Composable
fun MuscleZoneHierarchySelector(
    allZones: List<MuscleZone>,
    primaryZoneIds: List<Long>,
    secondaryZoneIds: List<Long>,
    onPrimaryZoneAdded: (Long) -> Unit,
    onPrimaryZoneRemoved: (Long) -> Unit,
    onSecondaryZoneAdded: (Long) -> Unit,
    onSecondaryZoneRemoved: (Long) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    // `null` = ningún diálogo abierto; `true` = el de principales; `false` = el de secundarias.
    var pickerForPrimary by remember { mutableStateOf<Boolean?>(null) }

    val zonesById = remember(allZones) { allZones.associateBy { it.id } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ZoneField(
            label = stringResource(R.string.exercise_field_primary_muscle_zones),
            isRequired = true,
            zoneIds = primaryZoneIds,
            zonesById = zonesById,
            addLabel = stringResource(R.string.muscle_zone_add_primary),
            onAdd = { pickerForPrimary = true },
            onRemove = onPrimaryZoneRemoved,
            error = error,
        )

        ZoneField(
            label = stringResource(R.string.exercise_field_secondary_muscle_zones),
            isRequired = false,
            zoneIds = secondaryZoneIds,
            zonesById = zonesById,
            addLabel = stringResource(R.string.muscle_zone_add_secondary),
            onAdd = { pickerForPrimary = false },
            onRemove = onSecondaryZoneRemoved,
            error = null,
        )
    }

    pickerForPrimary?.let { isPrimary ->
        MuscleZonePickerDialog(
            zones = allZones,
            alreadyChosenHere = if (isPrimary) primaryZoneIds.toSet() else secondaryZoneIds.toSet(),
            chosenInOtherField = if (isPrimary) secondaryZoneIds.toSet() else primaryZoneIds.toSet(),
            isPrimaryField = isPrimary,
            onZoneSelected = { zoneId ->
                if (isPrimary) onPrimaryZoneAdded(zoneId) else onSecondaryZoneAdded(zoneId)
                pickerForPrimary = null
            },
            onDismiss = { pickerForPrimary = null },
        )
    }
}

@Composable
private fun ZoneField(
    label: String,
    isRequired: Boolean,
    zoneIds: List<Long>,
    zonesById: Map<Long, MuscleZone>,
    addLabel: String,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    error: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isRequired) "$label *" else label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (!isRequired) {
                Text(
                    text = stringResource(R.string.exercise_field_optional_suffix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(FIELD_CORNER))
                .then(
                    if (error != null) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(FIELD_CORNER),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            if (zoneIds.isEmpty() && !isRequired) {
                Text(
                    text = stringResource(R.string.muscle_zone_empty_secondary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            zoneIds.forEach { zoneId ->
                val zone = zonesById[zoneId] ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ROW_MIN_HEIGHT)
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemove(zoneId) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(
                                R.string.muscle_zone_remove_description,
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT),
        ) {
            Text(text = addLabel)
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Zonas del ejercicio en la ficha, principales primero y visiblemente distintas (CA-41.02).
 *
 * Dos bloques y no una lista corrida: el ejecutante lee de un vistazo qué músculo ejecuta
 * el movimiento y cuál solo asiste, que es toda la razón por la que la jerarquía existe.
 */
@Composable
fun MuscleZoneHierarchySummary(
    primaryZones: List<String>,
    secondaryZones: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exercise_field_primary_muscle_zones),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = primaryZones.joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            text = stringResource(R.string.exercise_field_secondary_muscle_zones),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = secondaryZones.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                ?: stringResource(R.string.muscle_zone_empty_secondary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
