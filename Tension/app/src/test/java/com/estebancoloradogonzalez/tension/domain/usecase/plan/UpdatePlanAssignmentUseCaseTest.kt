package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdatePlanAssignmentUseCaseTest {

    private val repository: PlanRepository = mockk()
    private val useCase = UpdatePlanAssignmentUseCase(repository)

    @Test
    fun `invoke with valid numeric range reps delegates to repository`() = runTest {
        coEvery { repository.updatePlanAssignment(1L, 2L, 4, "8-12") } just runs

        useCase(1L, 2L, 4, "8-12")

        coVerify { repository.updatePlanAssignment(1L, 2L, 4, "8-12") }
    }

    @Test
    fun `invoke with TO_TECHNICAL_FAILURE reps delegates to repository`() = runTest {
        coEvery { repository.updatePlanAssignment(1L, 2L, 4, "TO_TECHNICAL_FAILURE") } just runs

        useCase(1L, 2L, 4, "TO_TECHNICAL_FAILURE")

        coVerify { repository.updatePlanAssignment(1L, 2L, 4, "TO_TECHNICAL_FAILURE") }
    }

    @Test
    fun `invoke with 30-45_SEC reps delegates to repository`() = runTest {
        coEvery { repository.updatePlanAssignment(1L, 2L, 4, "30-45_SEC") } just runs

        useCase(1L, 2L, 4, "30-45_SEC")

        coVerify { repository.updatePlanAssignment(1L, 2L, 4, "30-45_SEC") }
    }

    @Test
    fun `invoke with sets 1 succeeds`() = runTest {
        coEvery { repository.updatePlanAssignment(1L, 2L, 1, "8-12") } just runs

        useCase(1L, 2L, 1, "8-12")

        coVerify { repository.updatePlanAssignment(1L, 2L, 1, "8-12") }
    }

    @Test
    fun `invoke with sets 10 succeeds`() = runTest {
        coEvery { repository.updatePlanAssignment(1L, 2L, 10, "8-12") } just runs

        useCase(1L, 2L, 10, "8-12")

        coVerify { repository.updatePlanAssignment(1L, 2L, 10, "8-12") }
    }

    @Test
    fun `invoke with various valid ranges succeeds`() = runTest {
        coEvery { repository.updatePlanAssignment(any(), any(), any(), any()) } just runs

        useCase(1L, 2L, 3, "6-8")
        useCase(1L, 2L, 5, "3-5")
        useCase(1L, 2L, 4, "10-15")

        coVerify(exactly = 3) { repository.updatePlanAssignment(any(), any(), any(), any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with sets 0 throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 0, "8-12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with sets 11 throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 11, "8-12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with negative sets throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, -1, "8-12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with invalid reps format abc throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "abc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with reversed range throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "12-8")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with zero start range throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "0-5")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with blank reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with single number reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "8")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with equal range throws IllegalArgumentException`() = runTest {
        useCase(1L, 2L, 4, "8-8")
    }
}
