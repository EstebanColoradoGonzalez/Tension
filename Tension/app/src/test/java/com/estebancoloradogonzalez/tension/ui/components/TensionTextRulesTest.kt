package com.estebancoloradogonzalez.tension.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TensionTextRulesTest {

    @Test
    fun `given an entity name, when max lines are resolved, then two lines are allowed`() {
        // Given
        val kind = TensionTextKind.ENTITY_NAME

        // When
        val result = TensionTextRules.maxLinesFor(kind)

        // Then
        assertEquals(2, result)
    }

    @Test
    fun `given a counter, when max lines are resolved, then a single line is allowed`() {
        // Given
        val kind = TensionTextKind.COUNTER

        // When
        val result = TensionTextRules.maxLinesFor(kind)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `given an entity name, when truncation is checked, then it may be shortened`() {
        // Given
        val kind = TensionTextKind.ENTITY_NAME

        // When
        val result = TensionTextRules.isTruncatable(kind)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given a counter, when truncation is checked, then it is never shortened`() {
        // Given
        val kind = TensionTextKind.COUNTER

        // When
        val result = TensionTextRules.isTruncatable(kind)

        // Then
        assertFalse(result)
    }

    @Test
    fun `given a counter and an entity name, when max lines are compared, then the name yields first`() {
        // Given
        val nameLines = TensionTextRules.maxLinesFor(TensionTextKind.ENTITY_NAME)
        val counterLines = TensionTextRules.maxLinesFor(TensionTextKind.COUNTER)

        // When
        val nameCanYield = TensionTextRules.isTruncatable(TensionTextKind.ENTITY_NAME)
        val counterCanYield = TensionTextRules.isTruncatable(TensionTextKind.COUNTER)

        // Then
        assertTrue(nameLines > counterLines)
        assertTrue(nameCanYield)
        assertFalse(counterCanYield)
    }

    @Test
    fun `given every text kind, when the rule is applied, then all of them resolve a positive line limit`() {
        // Given
        val kinds = TensionTextKind.entries

        // When
        val limits = kinds.map { TensionTextRules.maxLinesFor(it) }

        // Then
        assertEquals(kinds.size, limits.size)
        assertTrue(limits.all { it >= 1 })
    }
}
