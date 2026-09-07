package com.estebancoloradogonzalez.tension.ui.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType

private val ROW_MIN_HEIGHT = 48.dp

/**
 * Selección múltiple de los implementos que un ejercicio admite (CA-39.03).
 *
 * Casillas de verificación y no chips ni desplegable: el campo es obligatorio y multivalor,
 * y la casilla no deja duda de qué está elegido y qué no. El coste asumido es que ocupa
 * alto de pantalla.
 *
 * **La lista se pinta entera, sin scroll propio.** El wireframe dibuja scroll dentro de la
 * sección, pero las dos pantallas que alojan este componente ya viven dentro de un
 * `verticalScroll`, y anidar dos desplazamientos en la misma dirección es un error de
 * Compose, no una decisión de diseño. Las 15 filas se recorren con el scroll del
 * formulario, que es el mismo gesto.
 *
 * El orden es el del catálogo y llega ya resuelto en [options]: `equipment_type.id` encoda
 * el orden declarado, así que este composable no ordena nada.
 *
 * [equipmentWithSets] marca con candado las opciones que no se pueden retirar porque
 * tienen series registradas (CA-39.10). El candado **no** deshabilita la fila: el
 * ejecutante puede intentarlo y recibir el motivo, que informa más que un control muerto.
 */
@Composable
fun EquipmentMultiSelector(
    options: List<EquipmentType>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    equipmentWithSets: Set<Long> = emptySet(),
    error: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            options.forEach { option ->
                val selected = option.id in selectedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ROW_MIN_HEIGHT)
                        .clickable { onToggle(option.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        // La fila entera es el área de toque; la casilla no la duplica.
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected && option.id in equipmentWithSets) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(
                                R.string.exercise_equipment_has_sets_description,
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(
                R.string.exercise_equipment_selected_count_format,
                selectedIds.size,
                options.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

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
