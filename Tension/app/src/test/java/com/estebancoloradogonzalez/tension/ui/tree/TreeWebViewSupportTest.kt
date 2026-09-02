package com.estebancoloradogonzalez.tension.ui.tree

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La comprobación de soporte del WebView (CA-38.05).
 *
 * Lo que se fija aquí es sobre todo la política ante lo desconocido: cuando la versión no se
 * puede leer, la respuesta es "no soportado". El fallback nativo siempre es una salida válida;
 * presumir soporte cuando no se puede comprobar no lo es.
 */
class TreeWebViewSupportTest {

    @Test
    fun `given a null version name, when support is checked, then it is not supported`() {
        assertFalse(TreeWebViewSupport.isSupportedVersion(null))
    }

    @Test
    fun `given an empty version name, when support is checked, then it is not supported`() {
        assertFalse(TreeWebViewSupport.isSupportedVersion(""))
    }

    @Test
    fun `given an unparseable version name, when support is checked, then it is not supported`() {
        assertFalse(TreeWebViewSupport.isSupportedVersion("no-es-una-version"))
    }

    @Test
    fun `given a version name with no numeric major, when support is checked, then it is not supported`() {
        assertFalse(TreeWebViewSupport.isSupportedVersion("beta.0.1.2"))
    }

    @Test
    fun `given a major just below the minimum, when support is checked, then it is not supported`() {
        val versionName = "${TreeWebViewSupport.MIN_WEBVIEW_MAJOR - 1}.0.4324.181"

        assertFalse(TreeWebViewSupport.isSupportedVersion(versionName))
    }

    @Test
    fun `given a major exactly at the minimum, when support is checked, then it is supported`() {
        val versionName = "${TreeWebViewSupport.MIN_WEBVIEW_MAJOR}.0.4430.210"

        assertTrue(TreeWebViewSupport.isSupportedVersion(versionName))
    }

    @Test
    fun `given a current WebView version name, when support is checked, then it is supported`() {
        assertTrue(TreeWebViewSupport.isSupportedVersion("120.0.6099.230"))
    }

    @Test
    fun `given a factory WebView of an old Android, when support is checked, then it is not supported`() {
        // Given — el WebView de fábrica de un Android 8 sin actualizar por tienda
        val versionName = "58.0.3029.125"

        // Then — el ejecutante verá el ícono nativo, que es el comportamiento correcto (RNF20)
        assertFalse(TreeWebViewSupport.isSupportedVersion(versionName))
    }

    @Test
    fun `given a version name with surrounding whitespace, when support is checked, then it is supported`() {
        assertTrue(TreeWebViewSupport.isSupportedVersion(" 120 .0.6099.230"))
    }

    @Test
    fun `given a major with no dots at all, when support is checked, then it is read as the major`() {
        assertTrue(TreeWebViewSupport.isSupportedVersion("131"))
    }
}
