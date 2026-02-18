package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class DeloadLoadRuleTest {

    // --- calculateDeloadLoad (60%) ---

    @Test
    fun `deload 60 pct of 60 Kg with increment 2_5 returns 35_0`() {
        // 60 × 0.60 = 36.0 → floor(36.0/2.5)×2.5 = 35.0
        assertEquals(35.0, DeloadLoadRule.calculateDeloadLoad(60.0, 2.5), 0.001)
    }

    @Test
    fun `deload 60 pct of 55 Kg with increment 2_5 returns 32_5`() {
        // 55 × 0.60 = 33.0 → floor(33.0/2.5)×2.5 = 32.5
        assertEquals(32.5, DeloadLoadRule.calculateDeloadLoad(55.0, 2.5), 0.001)
    }

    @Test
    fun `deload 60 pct of 100 Kg with increment 5_0 returns 60_0`() {
        // 100 × 0.60 = 60.0 → floor(60.0/5.0)×5.0 = 60.0
        assertEquals(60.0, DeloadLoadRule.calculateDeloadLoad(100.0, 5.0), 0.001)
    }

    @Test
    fun `deload 60 pct of 80 Kg with increment 5_0 returns 45_0`() {
        // 80 × 0.60 = 48.0 → floor(48.0/5.0)×5.0 = 45.0
        assertEquals(45.0, DeloadLoadRule.calculateDeloadLoad(80.0, 5.0), 0.001)
    }

    @Test
    fun `deload 60 pct of 2_5 Kg with increment 2_5 returns 0_0`() {
        // 2.5 × 0.60 = 1.5 → floor(1.5/2.5)×2.5 = 0.0
        assertEquals(0.0, DeloadLoadRule.calculateDeloadLoad(2.5, 2.5), 0.001)
    }

    @Test
    fun `deload 60 pct of 0 Kg returns 0_0`() {
        assertEquals(0.0, DeloadLoadRule.calculateDeloadLoad(0.0, 2.5), 0.001)
    }

    @Test
    fun `deload 60 pct of 62_5 Kg with increment 2_5 returns 37_5`() {
        // 62.5 × 0.60 = 37.5 → floor(37.5/2.5)×2.5 = 37.5 (exact)
        assertEquals(37.5, DeloadLoadRule.calculateDeloadLoad(62.5, 2.5), 0.001)
    }

    @Test
    fun `deload with increment 0_0 returns 0_0`() {
        assertEquals(0.0, DeloadLoadRule.calculateDeloadLoad(60.0, 0.0), 0.001)
    }

    // --- calculateResetLoad (90%) ---

    @Test
    fun `reset 90 pct of 60 Kg with increment 2_5 returns 52_5`() {
        // 60 × 0.90 = 54.0 → floor(54.0/2.5)×2.5 = 52.5
        assertEquals(52.5, DeloadLoadRule.calculateResetLoad(60.0, 2.5), 0.001)
    }

    @Test
    fun `reset 90 pct of 55 Kg with increment 2_5 returns 47_5`() {
        // 55 × 0.90 = 49.5 → floor(49.5/2.5)×2.5 = 47.5
        assertEquals(47.5, DeloadLoadRule.calculateResetLoad(55.0, 2.5), 0.001)
    }

    @Test
    fun `reset 90 pct of 100 Kg with increment 5_0 returns 90_0`() {
        // 100 × 0.90 = 90.0 → floor(90.0/5.0)×5.0 = 90.0
        assertEquals(90.0, DeloadLoadRule.calculateResetLoad(100.0, 5.0), 0.001)
    }

    @Test
    fun `reset 90 pct of 80 Kg with increment 5_0 returns 70_0`() {
        // 80 × 0.90 = 72.0 → floor(72.0/5.0)×5.0 = 70.0
        assertEquals(70.0, DeloadLoadRule.calculateResetLoad(80.0, 5.0), 0.001)
    }

    @Test
    fun `reset 90 pct of 0 Kg returns 0_0`() {
        assertEquals(0.0, DeloadLoadRule.calculateResetLoad(0.0, 2.5), 0.001)
    }

    @Test
    fun `reset 90 pct of 62_5 Kg with increment 2_5 returns 55_0`() {
        // 62.5 × 0.90 = 56.25 → floor(56.25/2.5)×2.5 = 55.0
        assertEquals(55.0, DeloadLoadRule.calculateResetLoad(62.5, 2.5), 0.001)
    }

    @Test
    fun `reset with increment 0_0 returns 0_0`() {
        assertEquals(0.0, DeloadLoadRule.calculateResetLoad(60.0, 0.0), 0.001)
    }
}
