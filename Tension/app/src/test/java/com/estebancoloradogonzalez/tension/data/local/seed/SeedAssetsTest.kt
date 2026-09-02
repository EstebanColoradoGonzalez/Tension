package com.estebancoloradogonzalez.tension.data.local.seed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la correspondencia 1:1 entre el `media_resource` de cada ejercicio del catálogo
 * y su archivo PNG en `assets/exercises/`, que es la ruta que compone `ExerciseDetailScreen`.
 */
class SeedAssetsTest {

    private val exercisesDir = File("src/main/assets/exercises")

    @Test
    fun `assets directory exists`() {
        assertTrue("No se encontró ${exercisesDir.absolutePath}", exercisesDir.isDirectory)
    }

    @Test
    fun `every media resource has its png file`() {
        ExerciseCatalog.ALL.forEach { exercise ->
            val file = File(exercisesDir, "${exercise.mediaResource}.png")
            assertTrue("Falta el recurso visual de ${exercise.name}: ${file.name}", file.isFile)
        }
    }

    @Test
    fun `exercises folder contains exactly 37 png files`() {
        val pngFiles = exercisesDir.listFiles { file -> file.extension == "png" }.orEmpty()
        assertEquals(37, pngFiles.size)
    }

    @Test
    fun `no orphan png without a catalog exercise`() {
        val expected = ExerciseCatalog.ALL.map { "${it.mediaResource}.png" }.toSet()
        val actual = exercisesDir.listFiles { file -> file.extension == "png" }.orEmpty().map { it.name }.toSet()
        assertEquals(emptySet<String>(), actual - expected)
    }
}
