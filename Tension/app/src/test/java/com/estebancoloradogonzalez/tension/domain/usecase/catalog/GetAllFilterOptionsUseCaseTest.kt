package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetAllFilterOptionsUseCaseTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var useCase: GetAllFilterOptionsUseCase

    @Before
    fun setUp() {
        exerciseRepository = mockk()
        useCase = GetAllFilterOptionsUseCase(exerciseRepository)
    }

    @Test
    fun `invoke combines all equipment types and all muscle zones`() = runTest {
        val equipmentTypes = listOf(
            EquipmentType(id = 1, name = "Máquina"),
            EquipmentType(id = 16, name = "Banda Elástica"),
        )
        val muscleZones = listOf(
            MuscleZone(id = 1, name = "Pecho Medio", muscleGroup = "Pecho"),
            MuscleZone(id = 16, name = "Espalda Alta", muscleGroup = "Espalda"),
        )

        every { exerciseRepository.getAllEquipmentTypes() } returns flowOf(equipmentTypes)
        every { exerciseRepository.getAllMuscleZones() } returns flowOf(muscleZones)

        val result = useCase().first()

        assertEquals(2, result.equipmentTypes.size)
        assertEquals("Máquina", result.equipmentTypes[0].name)
        assertEquals("Banda Elástica", result.equipmentTypes[1].name)
        assertEquals(2, result.muscleZones.size)
        assertEquals("Pecho Medio", result.muscleZones[0].name)
        assertEquals("Espalda Alta", result.muscleZones[1].name)
    }
}
