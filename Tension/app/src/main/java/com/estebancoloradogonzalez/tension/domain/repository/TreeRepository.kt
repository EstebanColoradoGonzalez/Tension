package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.TreeState
import kotlinx.coroutines.flow.Flow

/**
 * Estado visual del árbol de entrenamiento.
 *
 * **La dependencia es estrictamente unidireccional:** el árbol lee del historial de sesiones y
 * nada del sistema lee del árbol. Ningún componente del motor de decisión —prescripción de
 * carga, Doble Umbral, meseta, regresión, fatiga, protocolo de descarga, rotación cíclica—
 * consulta este contrato, el árbol no genera alertas y no altera ningún KPI.
 *
 * Vive separado de `SessionRepository` precisamente para que esa frontera sea legible en el
 * propio contrato y no solo en la documentación.
 */
interface TreeRepository {

    /** El estado actual del árbol, recompuesto cuando cambia lo persistido. */
    fun getTreeState(): Flow<TreeState>

    /** Recalcula salud y etapa desde el historial y las persiste. */
    suspend fun recalculate()
}
