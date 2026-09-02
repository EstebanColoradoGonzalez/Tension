package com.estebancoloradogonzalez.tension.domain.usecase.tree

import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTreeStateUseCaseTest {

    private val repository: TreeRepository = mockk()
    private val useCase = GetTreeStateUseCase(repository)

    @Test
    fun `invoke emits the repository state unchanged`() = runTest {
        val expected = TreeState(
            stage = TreeGrowthStage.YOUNG,
            healthScore = 50,
            daysSinceLastSession = 8,
        )
        every { repository.getTreeState() } returns flowOf(expected)

        assertEquals(expected, useCase().first())
    }

    // Sin historial el modelo lo declara, y es la senal que la interfaz consulta para no
    // mostrar un conteo de dias sin referencia.

    @Test
    fun `state without history reports no history`() = runTest {
        every { repository.getTreeState() } returns flowOf(
            TreeState(TreeGrowthStage.SEED, healthScore = 100, daysSinceLastSession = null),
        )

        assertFalse(useCase().first().hasHistory)
    }

    @Test
    fun `state with history reports history`() = runTest {
        every { repository.getTreeState() } returns flowOf(
            TreeState(TreeGrowthStage.SPROUT, healthScore = 100, daysSinceLastSession = 0),
        )

        assertTrue(useCase().first().hasHistory)
    }
}
