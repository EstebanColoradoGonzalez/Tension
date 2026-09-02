package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeGrowthStageRuleTest {

    @Test
    fun `zero sessions is a seed`() {
        assertEquals(TreeGrowthStage.SEED, TreeGrowthStageRule.resolve(0))
    }

    // Cortes inferior y superior de cada tramo.

    @Test
    fun `one session is a sprout`() {
        assertEquals(TreeGrowthStage.SPROUT, TreeGrowthStageRule.resolve(1))
    }

    @Test
    fun `nine sessions is still a sprout`() {
        assertEquals(TreeGrowthStage.SPROUT, TreeGrowthStageRule.resolve(9))
    }

    @Test
    fun `ten sessions is young`() {
        assertEquals(TreeGrowthStage.YOUNG, TreeGrowthStageRule.resolve(10))
    }

    @Test
    fun `twenty nine sessions is still young`() {
        assertEquals(TreeGrowthStage.YOUNG, TreeGrowthStageRule.resolve(29))
    }

    @Test
    fun `thirty sessions is mature`() {
        assertEquals(TreeGrowthStage.MATURE, TreeGrowthStageRule.resolve(30))
    }

    @Test
    fun `one hundred sessions is still mature`() {
        assertEquals(TreeGrowthStage.MATURE, TreeGrowthStageRule.resolve(100))
    }

    // La etapa no retrocede porque el conteo no baja: la funcion es monotona.

    @Test
    fun `stage never regresses as sessions accumulate`() {
        var previous = TreeGrowthStageRule.resolve(0).ordinal
        for (count in 1..60) {
            val current = TreeGrowthStageRule.resolve(count).ordinal
            assertTrue("n=$count retrocedio de $previous a $current", current >= previous)
            previous = current
        }
    }
}
