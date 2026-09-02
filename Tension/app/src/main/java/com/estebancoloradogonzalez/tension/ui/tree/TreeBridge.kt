package com.estebancoloradogonzalez.tension.ui.tree

import android.webkit.JavascriptInterface

/**
 * Puente JavaScript del árbol 3D, en su dirección web → nativa.
 *
 * **No transporta ningún dato de dominio.** El código web no devuelve datos al sistema: lo
 * único que cruza en esta dirección son dos señales sobre el estado del render, y ninguna de
 * las dos alimenta nada — solo deciden si se muestra el modelo 3D o el ícono nativo. La
 * dirección contraria, nativo → web, viaja por `evaluateJavascript` y lleva salud y etapa.
 *
 * Los métodos anotados con `@JavascriptInterface` los invoca el WebView desde su propio hilo,
 * nunca desde el principal. Todo se reenvía por [post], que en producción es el `Handler` del
 * hilo principal y en test es ejecución directa: es lo que hace verificable en JVM, sin
 * emulador, que [release] corta de verdad la referencia al listener.
 */
class TreeBridge(private val post: (() -> Unit) -> Unit) {

    /**
     * Quien escucha el resultado del render. Se anula en [release] para que el WebView, que
     * puede sobrevivir unos instantes a la pantalla, no retenga nada de ella.
     */
    interface Listener {
        fun onReady()
        fun onFailure(reason: String)
    }

    var listener: Listener? = null

    /** El primer fotograma se pintó: el modelo 3D puede sustituir al ícono nativo. */
    @JavascriptInterface
    fun onReady() {
        post { listener?.onReady() }
    }

    /**
     * El render no está disponible: sin WebGL, error de parseo o cualquier excepción del JS.
     *
     * @param reason texto de diagnóstico. No se presenta al ejecutante — la historia prohíbe
     *   mostrar mensaje de error ante fallo del WebView.
     */
    @JavascriptInterface
    fun onFailure(reason: String) {
        post { listener?.onFailure(reason) }
    }

    /**
     * Desengancha el listener. Se invoca **antes** de destruir el WebView: destruirlo con el
     * puente todavía registrado dejaría viva una referencia al listener, y el listener conoce
     * la pantalla.
     */
    fun release() {
        listener = null
    }

    companion object {
        /**
         * Nombre con el que el objeto se expone en `window`. La misma constante gobierna el
         * registro nativo y el uso desde `tree.js`, para que no puedan divergir.
         */
        const val NAME = "TreeBridge"
    }
}
