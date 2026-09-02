package com.estebancoloradogonzalez.tension.domain.usecase.tree

import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import javax.inject.Inject

/**
 * Recalcula y persiste salud y etapa del árbol.
 *
 * El estado del árbol depende de la fecha actual y del historial, así que se recalcula en tres
 * momentos:
 *
 * 1. **Al cerrar una sesión** — el árbol reacciona de inmediato al entrenamiento recién
 *    registrado.
 * 2. **En cada emisión del cambio de día** — cubre el arranque de la app y el cruce de la
 *    medianoche con la app abierta.
 * 3. **Al abrir la pantalla del árbol** — garantiza que lo mostrado nunca sea un valor rancio.
 *
 * En el caso 2 el recálculo corre **después** del barrido de cierre automático de sesiones del
 * día anterior, nunca antes. El barrido cierra la sesión de ayer conservando su fecha original;
 * recalcular primero leería una fecha de último entrenamiento desactualizada y marchitaría el
 * árbol de alguien que sí entrenó.
 */
class RecalculateTreeStateUseCase @Inject constructor(
    private val treeRepository: TreeRepository,
) {
    suspend operator fun invoke() {
        treeRepository.recalculate()
    }
}
