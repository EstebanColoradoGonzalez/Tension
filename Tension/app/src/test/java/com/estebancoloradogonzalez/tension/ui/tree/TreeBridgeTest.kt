package com.estebancoloradogonzalez.tension.ui.tree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El puente JavaScript del árbol 3D (CA-38.04).
 *
 * El puente recibe su `post` por constructor precisamente para poder probarse aquí: en
 * producción es el `Handler` del hilo principal, y en este test es ejecución directa. Lo que se
 * fija es el enrutado de las dos señales y, sobre todo, que `release()` corta de verdad la
 * referencia al listener — que es la verificación en JVM de «sin referencias retenidas al
 * contexto», sin necesidad de emulador ni de un profiler.
 */
class TreeBridgeTest {

    private class RecordingListener : TreeBridge.Listener {
        var readyCount = 0
        var failureReason: String? = null

        override fun onReady() {
            readyCount++
        }

        override fun onFailure(reason: String) {
            failureReason = reason
        }
    }

    /** `post` síncrono: el bloque se ejecuta en el mismo hilo, sin `Looper`. */
    private fun bridgeWith(listener: TreeBridge.Listener?): TreeBridge =
        TreeBridge(post = { block -> block() }).apply { this.listener = listener }

    @Test
    fun `given a listener, when the web side reports ready, then the listener is notified once`() {
        // Given
        val listener = RecordingListener()
        val bridge = bridgeWith(listener)

        // When
        bridge.onReady()

        // Then
        assertEquals(1, listener.readyCount)
        assertNull(listener.failureReason)
    }

    @Test
    fun `given a listener, when the web side reports failure, then the reason arrives untouched`() {
        // Given
        val listener = RecordingListener()
        val bridge = bridgeWith(listener)

        // When
        bridge.onFailure("THREE no está definido")

        // Then
        assertEquals("THREE no está definido", listener.failureReason)
        assertEquals(0, listener.readyCount)
    }

    @Test
    fun `given a released bridge, when the web side reports ready, then nobody is notified`() {
        // Given — es la situación real: el WebView puede sobrevivir unos instantes a la pantalla
        val listener = RecordingListener()
        val bridge = bridgeWith(listener)
        bridge.release()

        // When
        bridge.onReady()

        // Then
        assertEquals(0, listener.readyCount)
        assertNull(bridge.listener)
    }

    @Test
    fun `given a released bridge, when the web side reports failure, then nobody is notified`() {
        // Given
        val listener = RecordingListener()
        val bridge = bridgeWith(listener)
        bridge.release()

        // When
        bridge.onFailure("render process gone")

        // Then
        assertNull(listener.failureReason)
    }

    @Test
    fun `given no listener assigned, when the web side reports either signal, then nothing throws`() {
        // Given — el asset también se abre en un navegador para depurar la geometría
        val bridge = bridgeWith(listener = null)

        // When / Then — la ausencia de escucha no es un error
        bridge.onReady()
        bridge.onFailure("sin escucha")
    }

    @Test
    fun `given the bridge name, when it is read, then it matches the global exposed to JavaScript`() {
        // El mismo valor gobierna addJavascriptInterface y el uso desde tree.js: si divergen,
        // el árbol se queda mudo y nadie sabría por qué.
        assertEquals("TreeBridge", TreeBridge.NAME)
    }
}
