package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.Module
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

class GetFilterOptionsUseCaseTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var useCase: GetFilterOptionsUseCase

    @Before
    fun setUp() {
        exerciseRepository = mockk()
        useCase = GetFilterOptionsUseCase(exerciseRepository)
    }

    @Test
    fun `invoke combines modules, equipment types and muscle zones`() = runTest {
        val modules = listOf(
            Module(code = "A", name = "Módulo A — Superior", groupDescription = "Pecho, Espalda, Abdomen", loadIncrementKg = 2.5),
            Module(code = "B", name = "Módulo B — Superior", groupDescription = "Hombro, Tríceps, Bíceps", loadIncrementKg = 2.5),
        )
        val equipmentTypes = listOf(
            EquipmentType(id = 1, name = "Máquina"),
            EquipmentType(id = 2, name = "Mancuernas"),
        )
        val muscleZones = listOf(
            MuscleZone(id = 1, name = "Pecho Medio", muscleGroup = "Pecho"),
            MuscleZone(id = 2, name = "Abdomen", muscleGroup = "Abdomen"),
        )

        every { exerciseRepository.getAllModules() } returns flowOf(modules)
        every { exerciseRepository.getAllEquipmentTypes() } returns flowOf(equipmentTypes)
        every { exerciseRepository.getAllMuscleZones() } returns flowOf(muscleZones)

        val result = useCase().first()

        assertEquals(2, result.modules.size)
        assertEquals("A", result.modules[0].code)
        assertEquals(2, result.equipmentTypes.size)
        assertEquals("Máquina", result.equipmentTypes[0].name)
        assertEquals(2, result.muscleZones.size)
        assertEquals("Pecho Medio", result.muscleZones[0].name)
    }
}
