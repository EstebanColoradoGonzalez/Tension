package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CorrectSetUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = CorrectSetUseCase(repository)

    @Test
    fun `given valid values, when correcting, then the repository receives them`() = runTest {
        // Given — QA-05: 18 × 18 → 10 × 10
        coEvery { repository.correctSet(5L, 77L, 10.0, 10, 1, WeightUnit.KG) } just runs

        // When
        useCase(5L, 77L, 10.0, 10, 1, WeightUnit.KG, "Mancuerna")

        // Then
        coVerify { repository.correctSet(5L, 77L, 10.0, 10, 1, WeightUnit.KG) }
    }

    @Test
    fun `given a weight captured in lb, when correcting, then the canonical value and the unit travel`() =
        runTest {
            coEvery { repository.correctSet(5L, 77L, 49.9, 10, 1, WeightUnit.LB) } just runs

            useCase(5L, 77L, 49.9, 10, 1, WeightUnit.LB, "Barra")

            coVerify { repository.correctSet(5L, 77L, 49.9, 10, 1, WeightUnit.LB) }
        }

    @Test(expected = IllegalArgumentException::class)
    fun `given a negative weight, when correcting, then it is rejected before persisting`() = runTest {
        useCase(5L, 77L, -1.0, 10, 1, WeightUnit.KG, "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given zero reps, when correcting, then it is rejected before persisting`() = runTest {
        useCase(5L, 77L, 60.0, 0, 1, WeightUnit.KG, "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given an RIR out of range, when correcting, then it is rejected before persisting`() = runTest {
        useCase(5L, 77L, 60.0, 10, 3, WeightUnit.KG, "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given added weight of zero, when correcting, then it is rejected as in the registration`() =
        runTest {
            useCase(5L, 77L, 0.0, 10, 1, WeightUnit.KG, ExternalLoadRule.PESO_ANADIDO)
        }

    @Test(expected = IllegalStateException::class)
    fun `given the set is no longer the last one, when correcting, then the repository verdict propagates`() =
        runTest {
            // QA-12: the window is evaluated inside the transaction, not here.
            coEvery { repository.correctSet(any(), any(), any(), any(), any(), any()) } throws
                IllegalStateException("Set is no longer correctable")

            useCase(5L, 77L, 10.0, 10, 1, WeightUnit.KG, "Mancuerna")
        }
}
