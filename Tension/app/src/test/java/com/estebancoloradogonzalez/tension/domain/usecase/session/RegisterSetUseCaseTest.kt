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

private const val BARRA = 4L
private const val PESO_ANADIDO = 10L

class RegisterSetUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = RegisterSetUseCase(repository)

    @Test
    fun `invoke with valid data delegates to repository`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 2, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 60.0, 8, 2, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 60.0, 8, 2, WeightUnit.KG, BARRA) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with negative weight throws IllegalArgumentException`() = runTest {
        useCase(1L, -1.0, 8, 2, equipmentTypeId = BARRA)
    }

    @Test
    fun `invoke with zero weight succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 0.0, 8, 2, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 0.0, 8, 2, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 0.0, 8, 2, WeightUnit.KG, BARRA) }
    }

    @Test
    fun `invoke with the maximum weight succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 500.0, 8, 2, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 500.0, 8, 2, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 500.0, 8, 2, WeightUnit.KG, BARRA) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke above the maximum weight throws IllegalArgumentException`() = runTest {
        useCase(1L, 500.01, 8, 2, equipmentTypeId = BARRA)
    }

    @Test
    fun `invoke propagates the capture unit to the repository`() = runTest {
        coEvery { repository.registerSet(1L, 20.41, 10, 2, WeightUnit.LB, BARRA) } just runs

        useCase(1L, 20.41, 10, 2, WeightUnit.LB, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 20.41, 10, 2, WeightUnit.LB, BARRA) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with zero reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 0, 2, equipmentTypeId = BARRA)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with negative reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, -1, 2, equipmentTypeId = BARRA)
    }

    @Test
    fun `invoke with one rep succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 1, 2, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 60.0, 1, 2, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 60.0, 1, 2, WeightUnit.KG, BARRA) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with rir below zero throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 8, -1, equipmentTypeId = BARRA)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with rir above two throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 8, 3, equipmentTypeId = BARRA)
    }

    @Test
    fun `invoke with rir zero succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 0, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 60.0, 8, 0, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 60.0, 8, 0, WeightUnit.KG, BARRA) }
    }

    @Test
    fun `invoke with rir two succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 2, WeightUnit.KG, BARRA) } just runs

        useCase(1L, 60.0, 8, 2, equipmentTypeId = BARRA)

        coVerify { repository.registerSet(1L, 60.0, 8, 2, WeightUnit.KG, BARRA) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates exception when exercise already has max sets`() = runTest {
        coEvery { repository.registerSet(any(), any(), any(), any(), any(), any()) } throws
            IllegalStateException("Exercise already has maximum sets registered")

        useCase(1L, 60.0, 8, 2, equipmentTypeId = BARRA)
    }

    // CA-39.04 — el equipamiento es obligatorio

    @Test(expected = IllegalArgumentException::class)
    fun `given no equipment, when registering a set, then it is rejected`() = runTest {
        useCase(1L, 60.0, 8, 2, equipmentTypeId = 0L)
    }

    // CA-39.05 — el lastre debe ser mayor que 0

    @Test(expected = IllegalArgumentException::class)
    fun `given peso anadido with zero load, when registering a set, then it is rejected`() =
        runTest {
            useCase(
                sessionExerciseId = 1L,
                weightKg = 0.0,
                reps = 8,
                rir = 1,
                equipmentTypeId = PESO_ANADIDO,
                equipmentTypeName = ExternalLoadRule.PESO_ANADIDO,
            )
        }

    @Test
    fun `given peso anadido with positive load, when registering a set, then it is accepted`() =
        runTest {
            coEvery {
                repository.registerSet(1L, 10.0, 8, 1, WeightUnit.KG, PESO_ANADIDO)
            } just runs

            useCase(
                sessionExerciseId = 1L,
                weightKg = 10.0,
                reps = 8,
                rir = 1,
                equipmentTypeId = PESO_ANADIDO,
                equipmentTypeName = ExternalLoadRule.PESO_ANADIDO,
            )

            coVerify { repository.registerSet(1L, 10.0, 8, 1, WeightUnit.KG, PESO_ANADIDO) }
        }

    @Test
    fun `given peso corporal with zero load, when registering a set, then it is accepted`() =
        runTest {
            coEvery { repository.registerSet(1L, 0.0, 8, 1, WeightUnit.KG, 9L) } just runs

            useCase(
                sessionExerciseId = 1L,
                weightKg = 0.0,
                reps = 8,
                rir = 1,
                equipmentTypeId = 9L,
                equipmentTypeName = ExternalLoadRule.PESO_CORPORAL,
            )

            coVerify { repository.registerSet(1L, 0.0, 8, 1, WeightUnit.KG, 9L) }
        }
}
