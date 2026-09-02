package com.estebancoloradogonzalez.tension.domain.model

/**
 * Etapa de crecimiento del árbol de entrenamiento.
 *
 * Expresa el **historial acumulado** — cuántas sesiones se han registrado desde el principio —
 * y es ortogonal a la salud, que expresa la recencia. Un árbol maduro puede estar marchito y
 * un brote puede estar perfectamente sano.
 */
enum class TreeGrowthStage(val code: String) {
    SEED("SEED"),
    SPROUT("SPROUT"),
    YOUNG("YOUNG"),
    MATURE("MATURE"),
    ;

    companion object {
        /**
         * Un código desconocido —respaldo corrupto o de otra versión— cae en [SEED] en lugar
         * de reventar la pantalla: el árbol es una funcionalidad visual y su fallo no puede
         * costar más que mostrar el estado de partida.
         */
        fun fromCode(code: String): TreeGrowthStage =
            entries.firstOrNull { it.code == code } ?: SEED
    }
}
