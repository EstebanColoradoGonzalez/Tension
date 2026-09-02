package com.estebancoloradogonzalez.tension.ui.tree

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La predicción de calidad de render (CA-38.06).
 *
 * Es la única lógica con ramas que esta historia pone en Kotlin, y por eso es la única con
 * test: la geometría de `tree.js` se verifica a mano y la sonda de rendimiento corrige lo que
 * esta predicción se deje.
 */
class TreeRenderQualityTest {

    @Test
    fun `given a device the system declares low ram, when quality is resolved, then it is low`() {
        // Given — memoria y núcleos de sobra, pero el sistema dice que es gama baja
        val memoryClassMb = 512
        val processorCount = 8

        // When
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = memoryClassMb,
            isLowRamDevice = true,
            processorCount = processorCount,
        )

        // Then — cuando el propio sistema lo declara, no se discute
        assertEquals(TreeRenderQuality.LOW, quality)
    }

    @Test
    fun `given memory class just below the low cut, when quality is resolved, then it is low`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = TreeRenderQuality.LOW_MEMORY_CLASS_MB - 1,
            isLowRamDevice = false,
            processorCount = 8,
        )

        assertEquals(TreeRenderQuality.LOW, quality)
    }

    @Test
    fun `given memory class exactly at the low cut, when quality is resolved, then it is not low`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = TreeRenderQuality.LOW_MEMORY_CLASS_MB,
            isLowRamDevice = false,
            processorCount = 4,
        )

        assertEquals(TreeRenderQuality.MEDIUM, quality)
    }

    @Test
    fun `given processor count just below the low cut, when quality is resolved, then it is low`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = 512,
            isLowRamDevice = false,
            processorCount = TreeRenderQuality.LOW_PROCESSOR_COUNT - 1,
        )

        assertEquals(TreeRenderQuality.LOW, quality)
    }

    @Test
    fun `given both high cuts are met exactly, when quality is resolved, then it is high`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = TreeRenderQuality.HIGH_MEMORY_CLASS_MB,
            isLowRamDevice = false,
            processorCount = TreeRenderQuality.HIGH_PROCESSOR_COUNT,
        )

        assertEquals(TreeRenderQuality.HIGH, quality)
    }

    @Test
    fun `given enough memory but not enough cores, when quality is resolved, then it is medium`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = TreeRenderQuality.HIGH_MEMORY_CLASS_MB,
            isLowRamDevice = false,
            processorCount = TreeRenderQuality.HIGH_PROCESSOR_COUNT - 1,
        )

        assertEquals(TreeRenderQuality.MEDIUM, quality)
    }

    @Test
    fun `given enough cores but not enough memory, when quality is resolved, then it is medium`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = TreeRenderQuality.HIGH_MEMORY_CLASS_MB - 1,
            isLowRamDevice = false,
            processorCount = TreeRenderQuality.HIGH_PROCESSOR_COUNT,
        )

        assertEquals(TreeRenderQuality.MEDIUM, quality)
    }

    @Test
    fun `given a generous device, when quality is resolved, then it is high`() {
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = 512,
            isLowRamDevice = false,
            processorCount = 8,
        )

        assertEquals(TreeRenderQuality.HIGH, quality)
    }

    @Test
    fun `given an absent activity manager modeled as zero and low ram, when resolved, then it is low`() {
        // Given — es el valor con el que el composable resuelve un ActivityManager nulo
        val quality = TreeRenderQuality.resolve(
            memoryClassMb = 0,
            isLowRamDevice = true,
            processorCount = 1,
        )

        // Then — más vale un árbol simple de más que una pantalla trabada
        assertEquals(TreeRenderQuality.LOW, quality)
    }

    @Test
    fun `given every quality level, when its code is read, then it matches the query string value`() {
        assertEquals("high", TreeRenderQuality.HIGH.code)
        assertEquals("medium", TreeRenderQuality.MEDIUM.code)
        assertEquals("low", TreeRenderQuality.LOW.code)
    }
}
