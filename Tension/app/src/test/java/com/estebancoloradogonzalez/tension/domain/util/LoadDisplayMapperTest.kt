package com.estebancoloradogonzalez.tension.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadDisplayMapperTest {

    @Test
    fun `deload isometric returns isometric 30s`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = true, isBodyweight = false,
            prescribedLoadKg = null, muscleGroup = "Pecho",
        )
        assertEquals("Isométrico (30s)", result)
    }

    @Test
    fun `deload bodyweight returns bodyweight 8 reps`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = true,
            prescribedLoadKg = null, muscleGroup = "Pecho",
        )
        assertEquals("Peso corporal (8 reps objetivo)", result)
    }

    @Test
    fun `deload with prescribed load returns deload load`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 50.0, muscleGroup = "Pecho",
        )
        assertEquals("\uD83D\uDD04 30.0 Kg", result)
    }

    @Test
    fun `isometric without deload returns 30-45s`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = true, isBodyweight = false,
            prescribedLoadKg = null, muscleGroup = "Pecho",
        )
        assertEquals("Isométrico (30\u201345s)", result)
    }

    @Test
    fun `bodyweight without deload returns peso corporal`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = true,
            prescribedLoadKg = null, muscleGroup = "Pecho",
        )
        assertEquals("Peso corporal", result)
    }

    @Test
    fun `prescribed load returns formatted kg`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 42.5, muscleGroup = "Pecho",
        )
        assertEquals("42.5 Kg", result)
    }

    @Test
    fun `no history returns sin historial`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = false, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = null, muscleGroup = "Pecho",
        )
        assertEquals("Sin historial \u2014 establecer carga", result)
    }

    @Test
    fun `deload with lower body uses 5kg increment`() {
        val result = LoadDisplayMapper.mapLoadDisplay(
            isDeload = true, isIsometric = false, isBodyweight = false,
            prescribedLoadKg = 100.0, muscleGroup = "Cuádriceps",
        )
        // 100 * 0.60 / 5.0 = 12.0 -> floor(12.0) * 5.0 = 60.0
        assertEquals("\uD83D\uDD04 60.0 Kg", result)
    }
}
