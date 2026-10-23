package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-49 — the volume by muscle group (G2) adds the real load, not only the external one. */
class GetTonnageByMuscleGroupUseCaseTest {

    private val repository: MetricsRepository = mockk()
    private val useCase = GetTonnageByMuscleGroupUseCase(repository)

    private fun set(
        weightKg: Double,
        reps: Int,
        group: String,
        percent: Int,
        equipment: String,
        bodyWeightKg: Double? = 80.0,
    ) = SetTonnageData(
        weightKg = weightKg,
        reps = reps,
        muscleGroup = group,
        bodyWeightKg = bodyWeightKg,
        bodyWeightPercent = percent,
        isIsometric = false,
        equipmentTypeName = equipment,
    )

    @Test
    fun `given pull-ups on the bar and barbell rows, when grouped, then the back counts the body weight`() =
        runTest {
            // Given — QA-04: 23 pull-ups × 80 kg = 1840 kg; rows 27 reps × 30 kg = 810 kg
            coEvery { repository.getTonnageDataBySessionIds(listOf(1L)) } returns listOf(
                set(0.0, 8, "Espalda", 100, "Barra Fija"),
                set(0.0, 8, "Espalda", 100, "Barra Fija"),
                set(0.0, 7, "Espalda", 100, "Barra Fija"),
                set(30.0, 10, "Espalda", 0, "Barra"),
                set(30.0, 9, "Espalda", 0, "Barra"),
                set(30.0, 8, "Espalda", 0, "Barra"),
            )

            // When
            val result = useCase(listOf(1L))

            // Then
            assertEquals(2_650.0, result.first { it.muscleGroup == "Espalda" }.tonnageKg, 0.001)
        }

    @Test
    fun `given implements that do not add body weight, when grouped, then only the external load counts`() =
        runTest {
            // Given — QA-02: cable crunch 30 × 12 and machine crunch 25 × 12, assisted pull-up 0
            coEvery { repository.getTonnageDataBySessionIds(listOf(2L)) } returns listOf(
                set(30.0, 12, "Abdomen", 35, "Polea"),
                set(25.0, 12, "Abdomen", 35, "Máquina"),
                set(0.0, 10, "Espalda", 100, "Máquina"),
            )

            // When
            val result = useCase(listOf(2L))

            // Then
            assertEquals(660.0, result.first { it.muscleGroup == "Abdomen" }.tonnageKg, 0.001)
            assertEquals(0.0, result.first { it.muscleGroup == "Espalda" }.tonnageKg, 0.001)
        }
}
