package com.estebancoloradogonzalez.tension.ui.tree

/**
 * Si el WebView del dispositivo puede con el árbol 3D.
 *
 * El build de Three.js que viaja empaquetado usa sintaxis de JavaScript moderna, y un WebView
 * anterior a cierta versión no lo parsea. Esta comprobación descarta esos casos **antes** de
 * intentarlo, para que el ejecutante vea directamente el ícono nativo en lugar de un hueco
 * durante el tiempo que tarda el timeout.
 *
 * No es la única defensa ni pretende serlo: la relación entre versión de WebView y sintaxis
 * soportada no es exacta. Si a pesar de la comprobación el JS falla, `tree.js` lo reporta por
 * el puente y, si ni eso llega, el timeout nativo lleva al mismo sitio. Esto solo evita la
 * espera cuando el fallo es predecible.
 */
object TreeWebViewSupport {

    /**
     * Versión mayor mínima del paquete de WebView. Se corresponde con Chromium 90, la primera
     * rama que soporta con holgura la sintaxis del build de Three.js empaquetado.
     */
    const val MIN_WEBVIEW_MAJOR = 90

    /**
     * @param versionName el `versionName` del paquete de WebView del sistema, con la forma
     *   `"120.0.6099.230"`. Un valor nulo, vacío o cuyo primer segmento no sea un número se
     *   trata como **no soportado**: el fallback nativo es siempre una salida válida, y
     *   presumir soporte cuando no se puede comprobar no lo es.
     */
    fun isSupportedVersion(versionName: String?): Boolean {
        val major = versionName
            ?.substringBefore('.')
            ?.trim()
            ?.toIntOrNull()
            ?: return false

        return major >= MIN_WEBVIEW_MAJOR
    }
}
