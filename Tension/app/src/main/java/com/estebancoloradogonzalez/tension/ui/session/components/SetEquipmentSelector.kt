package com.estebancoloradogonzalez.tension.ui.session.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType

/**
 * Implemento con el que se registra la serie (CA-39.04).
 *
 * Solo ofrece las opciones que el ejercicio admite: `Máquina Smith` o `Barra` no aparecen
 * en un ejercicio que no las declara.
 *
 * **Con una sola opción se presenta resuelto y sin interacción.** No es un desplegable
 * deshabilitado: un control que se puede tocar para no elegir nada es peor que una
 * etiqueta. El dato queda igual de escrito en la serie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEquipmentSelector(
    options: List<EquipmentType>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val selectedName = options.firstOrNull { it.id == selectedId }?.name.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        if (options.size <= 1) {
            Text(
                text = stringResource(R.string.register_set_equipment_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = options.firstOrNull()?.name.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            return@Column
        }

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.register_set_equipment_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onSelected(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
