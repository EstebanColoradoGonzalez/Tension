package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHealthRuleTest {

    // Sin historial no se castiga: quien no ha entrenado nunca no ha faltado a nada.

    @Test
    fun `no history returns full health`() {
        assertEquals(100, TreeHealthRule.calculate(null))
    }

    // El margen de 48 horas: descansar un dia es parte del entrenamiento, no una falta.

    @Test
    fun `same day returns 100`() {
        assertEquals(100, TreeHealthRule.calculate(0))
    }

    @Test
    fun `one day returns 100`() {
        assertEquals(100, TreeHealthRule.calculate(1))
    }

    @Test
    fun `two days returns 100`() {
        assertEquals(100, TreeHealthRule.calculate(2))
    }

    // Los cinco puntos que la historia fija como verificacion del descenso lineal.

    @Test
    fun `three days returns 92`() {
        assertEquals(92, TreeHealthRule.calculate(3))
    }

    @Test
    fun `five days returns 75`() {
        assertEquals(75, TreeHealthRule.calculate(5))
    }

    @Test
    fun `eight days returns 50`() {
        assertEquals(50, TreeHealthRule.calculate(8))
    }

    @Test
    fun `eleven days returns 25`() {
        assertEquals(25, TreeHealthRule.calculate(11))
    }

    @Test
    fun `thirteen days returns 8`() {
        assertEquals(8, TreeHealthRule.calculate(13))
    }

    // El corte coincide con el umbral de crisis de ROUTINE_INACTIVITY.

    @Test
    fun `fourteen days returns 0`() {
        assertEquals(0, TreeHealthRule.calculate(14))
    }

    @Test
    fun `beyond fourteen days stays at 0`() {
        assertEquals(0, TreeHealthRule.calculate(30))
    }

    // Reloj movido hacia atras: el resultado sigue dentro del rango, no por debajo del maximo.

    @Test
    fun `negative days returns 100`() {
        assertEquals(100, TreeHealthRule.calculate(-1))
    }

    @Test
    fun `every result stays within 0 and 100`() {
        for (days in -5..40) {
            val score = TreeHealthRule.calculate(days)
            assertTrue("d=$days produjo $score", score in 0..100)
        }
    }

    // El descenso es monotono: mas dias nunca dan mas salud.

    @Test
    fun `health never increases as days grow`() {
        var previous = TreeHealthRule.calculate(0)
        for (days in 1..20) {
            val current = TreeHealthRule.calculate(days)
            assertTrue("d=$days subio de $previous a $current", current <= previous)
            previous = current
        }
    }
}
