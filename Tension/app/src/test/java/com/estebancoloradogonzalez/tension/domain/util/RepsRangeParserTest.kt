package com.estebancoloradogonzalez.tension.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RepsRangeParserTest {

    @Test
    fun `parse 30-45_SEC returns seconds range`() {
        val result = RepsRangeParser.parse("30-45_SEC")
        assertEquals(RepsRange(30, 45, true), result)
    }

    @Test
    fun `parse 8-12 returns non-seconds range`() {
        val result = RepsRangeParser.parse("8-12")
        assertEquals(RepsRange(8, 12, false), result)
    }

    @Test
    fun `parse TO_TECHNICAL_FAILURE returns isSeconds false`() {
        val result = RepsRangeParser.parse("TO_TECHNICAL_FAILURE")
        assertEquals(false, result.isSeconds)
    }

    @Test
    fun `parse 30-60_SEC returns correct range`() {
        val result = RepsRangeParser.parse("30-60_SEC")
        assertEquals(RepsRange(30, 60, true), result)
    }

    @Test
    fun `parse malformed input returns safe defaults`() {
        val result = RepsRangeParser.parse("abc")
        assertEquals(RepsRange(30, 60, false), result)
    }

    @Test
    fun `parse single number returns min with default max`() {
        val result = RepsRangeParser.parse("10")
        assertEquals(RepsRange(10, 60, false), result)
    }
}
