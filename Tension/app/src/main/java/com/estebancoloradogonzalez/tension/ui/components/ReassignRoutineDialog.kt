package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine

/** Área táctil mínima de cada opción (RNF06). */
private val OPTION_MIN_HEIGHT = 48.dp

/**
 * Selector de reasignación temporal de la rutina de hoy (HU-36).
 *
 * Se resuelve sobre la pantalla actual: no hay ruta nueva, y por eso el mismo diálogo sirve
 * a Inicio y al preview de sesión. El subtítulo declara de forma explícita que la
 * reasignación aplica solo a la sesión de hoy y que el plan no cambia.
 *
 * La opción que ya correspondía a hoy se marca y **no se excluye**: elegirla es válido y su
 * comportamiento es idéntico a no reasignar.
 */
@Composable
fun ReassignRoutineDialog(
    options: List<ReassignableRoutine>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialSelection = remember(options) {
        options.firstOrNull { it.isTodaysRoutine }?.routineId ?: options.firstOrNull()?.routineId
    }
    var selectedRoutineId by rememberSaveable(options) {
        mutableStateOf(initialSelection)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.reassign_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reassign_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.reassign_dialog_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        options.forEachIndexed { index, option ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            RoutineOption(
                                option = option,
                                isSelected = option.routineId == selectedRoutineId,
                                onSelected = { selectedRoutineId = option.routineId },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedRoutineId?.let(onConfirm) },
                enabled = selectedRoutineId != null,
            ) {
                Text(text = stringResource(R.string.reassign_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.reassign_dialog_cancel))
            }
        },
    )
}

@Composable
private fun RoutineOption(
    option: ReassignableRoutine,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OPTION_MIN_HEIGHT)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (option.weekDays.isNotEmpty()) {
                Text(
                    text = weekDaysLabel(option.weekDays),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EntityNameText(
                text = option.routineName,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (option.isTodaysRoutine) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.reassign_dialog_current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
