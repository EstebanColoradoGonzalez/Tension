package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryEntry
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetExerciseHistoryUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetExerciseHistoryUseCase(repository)

    @Test
    fun `invoke returns exercise history with entries`() = runTest {
        val historyData = ExerciseHistoryData(
            exerciseName = "Press Banca",
            progressionStatusByEquipment = mapOf("Barra" to "IN_PROGRESSION"),
            isBodyweight = false,
            isIsometric = false,
            equipmentOptions = listOf("Barra"),
            entries = listOf(
                ExerciseHistoryEntry(
                    date = "2026-02-15",
                    routineName = "Push",
                    versionNumber = 1,
                    equipmentTypeName = "Barra",
                    avgWeightKg = 60.0,
                    totalReps = 40,
                    avgRir = 2.0,
                    classification = ProgressionClassification.POSITIVE_PROGRESSION,
                ),
                ExerciseHistoryEntry(
                    date = "2026-02-10",
                    routineName = "Push",
                    versionNumber = 1,
                    equipmentTypeName = "Barra",
                    avgWeightKg = 57.5,
                    totalReps = 38,
                    avgRir = 2.5,
                    classification = ProgressionClassification.MAINTENANCE,
                ),
                ExerciseHistoryEntry(
                    date = "2026-02-05",
                    routineName = "Push",
                    versionNumber = 1,
                    equipmentTypeName = "Barra",
                    avgWeightKg = 55.0,
                    totalReps = 36,
                    avgRir = 3.0,
                    classification = null,
                ),
            ),
        )
        coEvery { repository.getExerciseHistory(10L) } returns historyData

        val result = useCase(10L)

        assertEquals(3, result.entries.size)
        assertEquals("Press Banca", result.exerciseName)
        assertEquals("IN_PROGRESSION", result.progressionStatusByEquipment["Barra"])
    }

    @Test
    fun `invoke returns exercise history with empty entries`() = runTest {
        val historyData = ExerciseHistoryData(
            exerciseName = "Nuevo Ejercicio",
            progressionStatusByEquipment = emptyMap(),
            isBodyweight = false,
            isIsometric = false,
            equipmentOptions = emptyList(),
            entries = emptyList(),
        )
        coEvery { repository.getExerciseHistory(99L) } returns historyData

        val result = useCase(99L)

        assertTrue(result.entries.isEmpty())
        assertTrue(result.progressionStatusByEquipment.isEmpty())
    }
}
