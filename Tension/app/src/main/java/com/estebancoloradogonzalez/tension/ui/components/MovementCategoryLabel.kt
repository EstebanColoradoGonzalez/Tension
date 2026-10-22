package com.estebancoloradogonzalez.tension.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory

/**
 * Recurso con el nombre de la categoría tal como lo lee el ejecutante (HU-46). La base guarda
 * el código del dominio cerrado, no la etiqueta: esta es la única traducción del proyecto,
 * para que el Diccionario, el plan, la sesión y los mensajes de conflicto no puedan divergir.
 * Devuelve el recurso y no el texto para que los ViewModels la usen al redactar mensajes.
 */
@StringRes
fun movementCategoryLabelRes(category: MovementCategory): Int = when (category) {
    MovementCategory.PUSH -> R.string.movement_category_push
    MovementCategory.PULL -> R.string.movement_category_pull
    MovementCategory.LEGS -> R.string.movement_category_legs
}

@Composable
fun movementCategoryName(category: MovementCategory): String =
    stringResource(movementCategoryLabelRes(category))

/**
 * Etiqueta compacta de la categoría, en mayúsculas, para la fila de cada rutina del plan y la
 * cabecera de la sesión activa. Es texto y no color: la categoría se lee sin depender del tono.
 */
@Composable
fun MovementCategoryTag(
    category: MovementCategory,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = movementCategoryName(category).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
