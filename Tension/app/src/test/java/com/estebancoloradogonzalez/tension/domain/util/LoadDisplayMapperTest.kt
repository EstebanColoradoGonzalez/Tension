package com.estebancoloradogonzalez.tension.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadDisplayMapperTest {

    @Test
    fun `deload isometric returns isometric 30s`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = true, isBodyweight = false,
            prescribedLoadKg = null, loadIncrementKg = 2.5,
        )
        assertEquals("Isométrico (30s)", result)
    }

    @Test
    fun `deload bodyweight returns bodyweight 8 reps`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = true,
            prescribedLoadKg = null, loadIncrementKg = 2.5,
        )
        assertEquals("Peso corporal (8 reps objetivo)", result)
    }

    @Test
    fun `deload with prescribed load returns deload load`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 50.0, loadIncrementKg = 2.5,
        )
        assertEquals("\uD83D\uDD04 30.0 Kg", result)
    }

    @Test
    fun `isometric without deload returns 30-45s`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = true, isBodyweight = false,
            prescribedLoadKg = null, loadIncrementKg = 2.5,
        )
        assertEquals("Isométrico (30\u201345s)", result)
    }

    @Test
    fun `bodyweight without deload returns peso corporal`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = true,
            prescribedLoadKg = null, loadIncrementKg = 2.5,
        )
        assertEquals("Peso corporal", result)
    }

    @Test
    fun `prescribed load returns formatted kg`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 42.5, loadIncrementKg = 2.5,
        )
        assertEquals("42.5 Kg", result)
    }

    @Test
    fun `no history returns sin historial`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = null, loadIncrementKg = 2.5,
        )
        assertEquals("Sin historial \u2014 establecer carga", result)
    }

    @Test
    fun `deload with zero load increment returns zero`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 50.0, loadIncrementKg = 0.0,
        )
        assertEquals("\uD83D\uDD04 0.0 Kg", result)
    }
}
