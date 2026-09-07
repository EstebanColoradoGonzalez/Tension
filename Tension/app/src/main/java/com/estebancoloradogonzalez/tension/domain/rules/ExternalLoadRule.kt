package com.estebancoloradogonzalez.tension.domain.rules

/**
 * Decide si una serie captura carga externa, a partir del implemento usado y de las
 * marcas del ejercicio (HU-39, CA-39.05).
 *
 * La decisión es del **implemento**, no de la marca del ejercicio. Aplicar la regla solo
 * sobre `is_bodyweight` dejaría a `Crunch Abdominal`, `Sentadilla Búlgara` y `Zancadas`
 * —que admiten `Peso Corporal` sin estar marcados como peso corporal— pidiendo un peso
 * para una serie hecha con el propio cuerpo.
 *
 * Compara contra el nombre del catálogo, que es dato semilla en español, igual que
 * [LoadIncrementResolver] compara contra el grupo muscular.
 */
object ExternalLoadRule {

    /** El propio cuerpo no es carga: la serie se registra en 0 Kg. */
    const val PESO_CORPORAL = "Peso Corporal"

    /** Lastre sobre un ejercicio de peso corporal. Lo único que habilita la captura. */
    const val PESO_ANADIDO = "Peso Añadido"

    /**
     * Si el ejecutante debe teclear un peso para esta serie.
     *
     * La cuarta rama es la que cubre `Dominadas`: `Barra Fija` es la dominada estricta
     * con el propio peso, y `Máquina` es la asistida, donde la carga puesta es un
     * contrapeso que **resta** esfuerzo. Registrar un contrapeso como peso invertiría el
     * significado del dato y contaminaría el tonelaje y la progresión. Sobre cualquier
     * otro ejercicio `Máquina` sí es carga y la rama no se alcanza.
     */
    fun isCaptureEnabled(
        isBodyweight: Boolean,
        isIsometric: Boolean,
        equipmentName: String?,
    ): Boolean = when {
        isIsometric -> false
        equipmentName == PESO_CORPORAL -> false
        equipmentName == PESO_ANADIDO -> true
        isBodyweight -> false
        else -> true
    }

    /** El lastre debe ser estrictamente mayor que 0: sin lastre no hay peso añadido. */
    fun requiresPositiveLoad(equipmentName: String?): Boolean = equipmentName == PESO_ANADIDO
}
