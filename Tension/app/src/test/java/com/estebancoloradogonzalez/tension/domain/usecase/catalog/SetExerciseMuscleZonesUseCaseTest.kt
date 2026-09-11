package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val EXERCISE_ID = 18L
private const val PECTORAL_SUPERIOR = 2L
private const val DELTOIDES_ANTERIOR = 22L
private const val TRICEPS_BRAQUIAL = 8L

/** CA-41.09 — las zonas musculares y su jerarquía al editar un ejercicio. */
class SetExerciseMuscleZonesUseCaseTest {

    private val repository: ExerciseRepository = mockk(relaxUnitFun = true)
    private val useCase = SetExerciseMuscleZonesUseCase(repository)

    @Test
    fun `given a primary zone, when saving, then it is persisted`() = runTest {
        val result = useCase(
            exerciseId = EXERCISE_ID,
            primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR, TRICEPS_BRAQUIAL),
        )

        assertEquals(SetExerciseMuscleZonesUseCase.Result.Saved, result)
        coVerify {
            repository.setMuscleZones(
                exerciseId = EXERCISE_ID,
                primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR),
                secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR, TRICEPS_BRAQUIAL),
            )
        }
    }

    @Test
    fun `given no primary zone, when saving, then it is rejected without writing`() = runTest {
        val result = useCase(
            exerciseId = EXERCISE_ID,
            primaryMuscleZoneIds = emptyList(),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR),
        )

        assertEquals(SetExerciseMuscleZonesUseCase.Result.NoPrimaryZone, result)
        // Que no se escriba es la mitad del contrato: la ficha deriva su estado del flujo
        // de Room, y si el rechazo escribiera el campo no volvería a lo que había.
        coVerify(exactly = 0) { repository.setMuscleZones(any(), any(), any()) }
    }

    @Test
    fun `secondary zones are optional`() = runTest {
        val result = useCase(
            exerciseId = EXERCISE_ID,
            primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR),
            secondaryMuscleZoneIds = emptyList(),
        )

        assertEquals(SetExerciseMuscleZonesUseCase.Result.Saved, result)
    }

    @Test
    fun `given a zone in both lists, when saving, then it is rejected without writing`() = runTest {
        val result = useCase(
            exerciseId = EXERCISE_ID,
            primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR, DELTOIDES_ANTERIOR),
            secondaryMuscleZoneIds = listOf(DELTOIDES_ANTERIOR),
        )

        assertEquals(SetExerciseMuscleZonesUseCase.Result.ZoneInBothLists, result)
        coVerify(exactly = 0) { repository.setMuscleZones(any(), any(), any()) }
    }

    @Test
    fun `duplicated zones within a list are collapsed`() = runTest {
        useCase(
            exerciseId = EXERCISE_ID,
            primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR, PECTORAL_SUPERIOR),
            secondaryMuscleZoneIds = emptyList(),
        )

        // La PK de exercise_muscle_zone lo colapsaría de todos modos, pero enviar la
        // duplicada dejaría el conteo de la interfaz mintiendo hasta el siguiente repintado.
        coVerify {
            repository.setMuscleZones(
                exerciseId = EXERCISE_ID,
                primaryMuscleZoneIds = listOf(PECTORAL_SUPERIOR),
                secondaryMuscleZoneIds = emptyList(),
            )
        }
    }

    @Test
    fun `repository failure is not swallowed`() = runTest {
        coEvery { repository.setMuscleZones(any(), any(), any()) } throws IllegalStateException("db")

        val thrown = runCatching {
            useCase(EXERCISE_ID, listOf(PECTORAL_SUPERIOR), emptyList())
        }.exceptionOrNull()

        assertEquals("db", thrown?.message)
    }
}
