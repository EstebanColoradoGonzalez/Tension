package com.estebancoloradogonzalez.tension.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RepsDisplayMapperTest {

    @Test
    fun `map TO_TECHNICAL_FAILURE returns al fallo tecnico`() {
        val result = RepsDisplayMapper.mapRepsToDisplay("TO_TECHNICAL_FAILURE")
        assertEquals("Al fallo técnico" to true, result)
    }

    @Test
    fun `map 30-45_SEC returns formatted seconds`() {
        val result = RepsDisplayMapper.mapRepsToDisplay("30-45_SEC")
        assertEquals("30\u201345 seg" to true, result)
    }

    @Test
    fun `map standard reps returns reps format`() {
        val result = RepsDisplayMapper.mapRepsToDisplay("8-12")
        assertEquals("8-12 reps" to false, result)
    }

    @Test
    fun `map 30-60_SEC returns formatted seconds`() {
        val result = RepsDisplayMapper.mapRepsToDisplay("30-60_SEC")
        assertEquals("30\u201360 seg" to true, result)
    }
}
