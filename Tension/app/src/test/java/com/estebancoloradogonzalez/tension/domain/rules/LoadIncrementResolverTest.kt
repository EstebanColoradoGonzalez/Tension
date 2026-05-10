package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadIncrementResolverTest {

    @Test
    fun `upper body group Pecho returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Pecho"), 0.001)
    }

    @Test
    fun `upper body group Espalda returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Espalda"), 0.001)
    }

    @Test
    fun `upper body group Hombro returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Hombro"), 0.001)
    }

    @Test
    fun `upper body group Biceps returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Bíceps"), 0.001)
    }

    @Test
    fun `upper body group Triceps returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Tríceps"), 0.001)
    }

    @Test
    fun `upper body group Abdomen returns 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Abdomen"), 0.001)
    }

    @Test
    fun `lower body group Cuadriceps returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Cuádriceps"), 0.001)
    }

    @Test
    fun `lower body group Isquiotibiales returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Isquiotibiales"), 0.001)
    }

    @Test
    fun `lower body group Gluteos returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Glúteos"), 0.001)
    }

    @Test
    fun `lower body group Aductores returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Aductores"), 0.001)
    }

    @Test
    fun `lower body group Abductores returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Abductores"), 0.001)
    }

    @Test
    fun `lower body group Gemelos returns 5_0`() {
        assertEquals(5.0, LoadIncrementResolver.resolve("Gemelos"), 0.001)
    }

    @Test
    fun `unknown muscle group falls back to 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve("Desconocido"), 0.001)
    }

    @Test
    fun `null muscle group falls back to 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve(null), 0.001)
    }

    @Test
    fun `empty string muscle group falls back to 2_5`() {
        assertEquals(2.5, LoadIncrementResolver.resolve(""), 0.001)
    }
}
