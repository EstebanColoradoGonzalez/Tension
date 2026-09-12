package com.estebancoloradogonzalez.tension.domain.usecase.onerm

import com.estebancoloradogonzalez.tension.domain.model.OneRmEntry
import com.estebancoloradogonzalez.tension.domain.repository.OneRmRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetOneRmListUseCaseTest {

    private val repository: OneRmRepository = mockk()
    private val useCase = GetOneRmListUseCase(repository)

    private fun entry(
        exerciseId: Long,
        exerciseName: String,
        equipmentTypeId: Long,
        equipmentTypeName: String,
        oneRmKg: Double,
    ) = OneRmEntry(exerciseId, exerciseName, equipmentTypeId, equipmentTypeName, oneRmKg)

    // CA-42.02 — una tarjeta por ejercicio, con dentro los implementos que tienen valor.

    @Test
    fun `given rows of the same exercise, when invoked, then they collapse into one card`() =
        runTest {
            every { repository.getAll() } returns flowOf(
                listOf(
                    entry(10L, "Elevación Lateral", 1L, "Mancuerna", 16.0),
                    entry(10L, "Elevación Lateral", 2L, "Polea", 22.7),
                ),
            )

            val cards = useCase().first()

            assertEquals(1, cards.size)
            assertEquals("Elevación Lateral", cards.first().exerciseName)
            assertEquals(2, cards.first().equipment.size)
        }

    /**
     * El orden llega resuelto de SQL —alfabetico por ejercicio, y dentro por posicion
     * declarada del implemento— y agrupar no puede alterarlo.
     */
    @Test
    fun `given ordered rows, when invoked, then grouping preserves the incoming order`() =
        runTest {
            every { repository.getAll() } returns flowOf(
                listOf(
                    entry(1L, "Aperturas", 3L, "Mancuerna", 33.3),
                    entry(1L, "Aperturas", 5L, "Máquina", 28.0),
                    entry(2L, "Curl Martillo", 3L, "Mancuerna", 21.3),
                    entry(3L, "Jalón al Pecho", 4L, "Polea", 73.4),
                ),
            )

            val cards = useCase().first()

            assertEquals(
                listOf("Aperturas", "Curl Martillo", "Jalón al Pecho"),
                cards.map { it.exerciseName },
            )
            assertEquals(listOf(3L, 5L), cards.first().equipment.map { it.equipmentTypeId })
        }

    @Test
    fun `given a single implement, when invoked, then the card does not ask for a choice`() =
        runTest {
            every { repository.getAll() } returns flowOf(
                listOf(entry(2L, "Curl Martillo", 3L, "Mancuerna", 21.3)),
            )

            assertFalse(useCase().first().first().isSelectable)
        }

    @Test
    fun `given two implements, when invoked, then the card asks for a choice`() = runTest {
        every { repository.getAll() } returns flowOf(
            listOf(
                entry(10L, "Elevación Lateral", 1L, "Mancuerna", 16.0),
                entry(10L, "Elevación Lateral", 2L, "Polea", 22.7),
            ),
        )

        assertTrue(useCase().first().first().isSelectable)
    }

    /**
     * CA-42.07 — sin ninguna serie que haya calificado no hay filas, y el caso de uso no
     * inventa ninguna: quien explica el vacio es la pantalla.
     */
    @Test
    fun `given no records, when invoked, then the list is empty`() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `given a record, when invoked, then its value travels unchanged`() = runTest {
        every { repository.getAll() } returns flowOf(
            listOf(entry(3L, "Jalón al Pecho", 4L, "Polea", 73.4)),
        )

        assertEquals(73.4, useCase().first().first().equipment.first().oneRmKg, 0.001)
    }
}
