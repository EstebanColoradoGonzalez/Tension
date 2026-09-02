package com.estebancoloradogonzalez.tension.ui.tree

/**
 * Nivel de fidelidad con el que se genera el árbol 3D.
 *
 * El presupuesto de rendimiento manda sobre la fidelidad visual: si un dispositivo no alcanza
 * el objetivo, se degradan los gráficos antes que entregar una experiencia lenta. El orden de
 * degradación es sombras → esferas de la copa → segmentos del tronco → polígonos por
 * primitiva, y lo materializa la tabla de calidad de `assets/tree/tree.js`.
 *
 * [code] es el valor que viaja como *query string* al cargar el asset. La calidad **no** cruza
 * el puente de estado: es una propiedad del dispositivo que se fija una vez, mientras que el
 * puente transporta solo salud y etapa.
 */
enum class TreeRenderQuality(val code: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    ;

    companion object {

        /** Por debajo de esta clase de memoria el dispositivo se trata como gama baja. */
        const val LOW_MEMORY_CLASS_MB = 128

        /** Desde esta clase de memoria se habilita la fidelidad máxima. */
        const val HIGH_MEMORY_CLASS_MB = 256

        /** Por debajo de este número de núcleos el dispositivo se trata como gama baja. */
        const val LOW_PROCESSOR_COUNT = 4

        /** Desde este número de núcleos se habilita la fidelidad máxima. */
        const val HIGH_PROCESSOR_COUNT = 8

        /**
         * Predice la calidad a partir de las señales que el sistema expone del dispositivo.
         *
         * Es una función **pura sobre tres primitivas**: la lectura del `ActivityManager` vive
         * en el composable, no aquí, para que la decisión sea verificable en JVM sin emulador.
         *
         * La predicción puede equivocarse —el hardware no se deja resumir en tres números—, y
         * por eso no es la única defensa: la sonda de rendimiento de `tree.js` mide los
         * primeros fotogramas reales y baja un escalón si el presupuesto no se cumple. Esto
         * decide el punto de partida; la medida decide el final.
         *
         * @param memoryClassMb `ActivityManager.memoryClass` — memoria por proceso en MB.
         * @param isLowRamDevice `ActivityManager.isLowRamDevice` — el propio sistema declarando
         *   que el dispositivo es de gama baja. Cuando lo dice, no se discute.
         * @param processorCount núcleos disponibles para el proceso.
         */
        fun resolve(
            memoryClassMb: Int,
            isLowRamDevice: Boolean,
            processorCount: Int,
        ): TreeRenderQuality = when {
            isLowRamDevice -> LOW
            memoryClassMb < LOW_MEMORY_CLASS_MB -> LOW
            processorCount < LOW_PROCESSOR_COUNT -> LOW
            memoryClassMb >= HIGH_MEMORY_CLASS_MB && processorCount >= HIGH_PROCESSOR_COUNT -> HIGH
            else -> MEDIUM
        }
    }
}
