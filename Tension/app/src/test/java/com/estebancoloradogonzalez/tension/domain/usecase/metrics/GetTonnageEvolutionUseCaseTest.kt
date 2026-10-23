package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-49 — the tonnage evolution uses the real load with the body weight in force on each date. */
class GetTonnageEvolutionUseCaseTest {

    private val repository: MetricsRepository = mockk()
    private val useCase = GetTonnageEvolutionUseCase(repository)

    private fun pullUps(reps: Int, bodyWeightKg: Double) = SetTonnageData(
        weightKg = 0.0,
        reps = reps,
        muscleGroup = "Espalda",
        bodyWeightKg = bodyWeightKg,
        bodyWeightPercent = 100,
        isIsometric = false,
        equipmentTypeName = "Barra Fija",
    )

    private fun rows(weightKg: Double, reps: Int) = SetTonnageData(
        weightKg = weightKg,
        reps = reps,
        muscleGroup = "Espalda",
        bodyWeightKg = 80.0,
        bodyWeightPercent = 0,
        isIsometric = false,
        equipmentTypeName = "Barra",
    )

    @Test
    fun `given two microcycles, when evolved, then each one weighs its real load`() = runTest {
        // Given — QA-05: previous 3 × 8 pull-ups + 600 kg of rows = 2520; current 3 × 6 + 600 = 2040
        coEvery { repository.getTonnageDataBySessionIds(listOf(1L)) } returns
            List(3) { pullUps(8, 80.0) } + rows(30.0, 20)
        coEvery { repository.getTonnageDataBySessionIds(listOf(2L)) } returns
            List(3) { pullUps(6, 80.0) } + rows(30.0, 20)

        // When
        val snapshots = useCase(mapOf(1 to listOf(1L), 2 to listOf(2L)))

        // Then — −19 %: the drop alert is raised over these values, not over 600 → 600
        assertEquals(2_520.0, snapshots[0].tonnageByGroup.getValue("Espalda"), 0.001)
        assertEquals(2_040.0, snapshots[1].tonnageByGroup.getValue("Espalda"), 0.001)
    }

    @Test
    fun `given sessions on dates with different body weights, when evolved, then each uses its own`() =
        runTest {
            // Given — QA-06: 8 pull-ups with 78 kg (05/10) and with 80 kg (14/10)
            coEvery { repository.getTonnageDataBySessionIds(listOf(1L)) } returns listOf(pullUps(8, 78.0))
            coEvery { repository.getTonnageDataBySessionIds(listOf(2L)) } returns listOf(pullUps(8, 80.0))

            // When
            val snapshots = useCase(mapOf(1 to listOf(1L), 2 to listOf(2L)))

            // Then
            assertEquals(624.0, snapshots[0].tonnageByGroup.getValue("Espalda"), 0.001)
            assertEquals(640.0, snapshots[1].tonnageByGroup.getValue("Espalda"), 0.001)
        }
}
