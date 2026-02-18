package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendClassificationRuleTest {

    @Test
    fun `ascending values classified as ASCENDING`() {
        assertEquals(
            TrendDirection.ASCENDING,
            TrendClassificationRule.classify(listOf(100.0, 110.0, 120.0, 130.0)),
        )
    }

    @Test
    fun `constant values classified as STABLE`() {
        assertEquals(
            TrendDirection.STABLE,
            TrendClassificationRule.classify(listOf(100.0, 100.0, 100.0, 100.0)),
        )
    }

    @Test
    fun `descending values classified as DECLINING`() {
        assertEquals(
            TrendDirection.DECLINING,
            TrendClassificationRule.classify(listOf(130.0, 120.0, 110.0, 100.0)),
        )
    }

    @Test
    fun `single value classified as STABLE`() {
        assertEquals(
            TrendDirection.STABLE,
            TrendClassificationRule.classify(listOf(100.0)),
        )
    }

    @Test
    fun `small variation within 5 pct threshold classified as STABLE`() {
        assertEquals(
            TrendDirection.STABLE,
            TrendClassificationRule.classify(listOf(100.0, 101.0, 99.0, 100.0)),
        )
    }

    @Test
    fun `empty list classified as STABLE`() {
        assertEquals(
            TrendDirection.STABLE,
            TrendClassificationRule.classify(emptyList()),
        )
    }

    @Test
    fun `two ascending values classified as ASCENDING`() {
        assertEquals(
            TrendDirection.ASCENDING,
            TrendClassificationRule.classify(listOf(100.0, 200.0)),
        )
    }

    @Test
    fun `six microcycles ascending classified as ASCENDING`() {
        assertEquals(
            TrendDirection.ASCENDING,
            TrendClassificationRule.classify(
                listOf(500.0, 550.0, 600.0, 650.0, 700.0, 750.0),
            ),
        )
    }

    @Test
    fun `six microcycles declining classified as DECLINING`() {
        assertEquals(
            TrendDirection.DECLINING,
            TrendClassificationRule.classify(
                listOf(750.0, 700.0, 650.0, 600.0, 550.0, 500.0),
            ),
        )
    }
}
