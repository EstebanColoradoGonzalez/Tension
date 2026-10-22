package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSummaryDto
import com.estebancoloradogonzalez.tension.data.local.dao.SessionPairSummaryDto
import com.estebancoloradogonzalez.tension.data.local.dao.SessionSummaryInfo
import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.domain.model.ActionSignal
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSessionSummaryUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetSessionSummaryUseCase(repository)

    private val sessionId = 7L
    private val sessionExerciseId = 70L

    private val info = SessionSummaryInfo(
        status = "COMPLETED",
        routineName = "Push",
        versionNumber = 1,
        routineId = 1L,
        totalTonnageKg = 2148.5,
        totalExercises = 4,
        completedExercises = 4,
    )

    private fun exercise(
        classification: String? = "POSITIVE_PROGRESSION",
        avgWeightKg: Double = 10.0,
        setCount: Int = 4,
    ) = ExerciseSummaryDto(
        sessionExerciseId = sessionExerciseId,
        exerciseId = 10L,
        exerciseName = "Elevación Lateral",
        classification = classification,
        isBodyweight = 0,
        isIsometric = 0,
        avgWeightKg = avgWeightKg,
        totalReps = 40,
        setCount = setCount,
        prescribedSets = 4,
        isMastered = 0,
        muscleGroup = "Hombro",
        previousTotalReps = 38,
        isDeload = 0,
    )

    private fun pair(
        equipmentTypeId: Long,
        equipmentTypeName: String,
        classification: String?,
        prescribedLoadKg: Double? = null,
        avgWeightKg: Double = 10.0,
        setCount: Int = 2,
        rangeReading: String? = null,
        topRangeStreak: Int? = null,
        admitsAddedWeight: Boolean = false,
    ) = SessionPairSummaryDto(
        sessionExerciseId = sessionExerciseId,
        exerciseId = 10L,
        equipmentTypeId = equipmentTypeId,
        equipmentTypeName = equipmentTypeName,
        classification = classification,
        prescribedLoadKg = prescribedLoadKg,
        avgWeightKg = avgWeightKg,
        totalReps = 20,
        setCount = setCount,
        previousTotalReps = 19,
        rangeReading = rangeReading,
        topRangeStreak = topRangeStreak,
        admitsAddedWeight = admitsAddedWeight,
    )

    // ----- CA-40.02: un renglón por implemento ejecutado -----

    @Test
    fun `given two implements in one session, when building the summary, then there are two lines`() =
        runTest {
            // Given — mancuerna progresó y polea se estrena
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise()),
                pairs = listOf(
                    pair(6L, "Mancuerna", "POSITIVE_PROGRESSION", avgWeightKg = 12.0),
                    pair(8L, "Polea", null, avgWeightKg = 8.0),
                ),
                routineRequiresDeload = false,
            )

            // When
            val summary = useCase(sessionId)

            // Then
            val item = summary.exercises.single()
            assertEquals(2, item.pairs.size)
            assertEquals("Mancuerna", item.pairs[0].equipmentTypeName)
            assertEquals(
                ProgressionClassification.POSITIVE_PROGRESSION,
                item.pairs[0].classification,
            )
            // El estreno de la polea es Sin Historial, nunca una regresión
            assertEquals("Polea", item.pairs[1].equipmentTypeName)
            assertNull(item.pairs[1].classification)
            assertTrue(item.pairs[1].signal is ActionSignal.FirstSession)
        }

    @Test
    fun `given the pairs classify differently, when building the summary, then each keeps its own weight`() =
        runTest {
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise()),
                pairs = listOf(
                    pair(6L, "Mancuerna", "POSITIVE_PROGRESSION", avgWeightKg = 12.0),
                    pair(8L, "Polea", "MAINTENANCE", avgWeightKg = 8.0),
                ),
                routineRequiresDeload = false,
            )

            val item = useCase(sessionId).exercises.single()

            assertEquals(12.0, item.pairs[0].weightKg, 0.001)
            assertEquals(8.0, item.pairs[1].weightKg, 0.001)
        }

    // ----- La carga prescrita mostrada por par es la del par -----

    @Test
    fun `given each pair has its own prescription, when building the signal, then it uses the pair's`() =
        runTest {
            // Given — el Doble Umbral prescribe 15 kg a la mancuerna y 10 a la polea
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise()),
                pairs = listOf(
                    pair(
                        6L,
                        "Mancuerna",
                        "POSITIVE_PROGRESSION",
                        prescribedLoadKg = 15.0,
                        avgWeightKg = 12.5,
                    ),
                    pair(
                        8L,
                        "Polea",
                        "POSITIVE_PROGRESSION",
                        prescribedLoadKg = 10.0,
                        avgWeightKg = 8.0,
                    ),
                ),
                routineRequiresDeload = false,
            )

            // When
            val item = useCase(sessionId).exercises.single()

            // Then — cada renglón sube a su propio objetivo, no al del otro implemento
            assertEquals(
                15.0,
                (item.pairs[0].signal as ActionSignal.IncreaseLoad).targetKg,
                0.001,
            )
            assertEquals(
                10.0,
                (item.pairs[1].signal as ActionSignal.IncreaseLoad).targetKg,
                0.001,
            )
        }

    // ----- CA-40.08: con un solo implemento la pantalla es la de siempre -----

    @Test
    fun `given a single implement, when building the summary, then there is one pair and the same signal`() =
        runTest {
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise(avgWeightKg = 70.0)),
                pairs = listOf(
                    pair(
                        4L,
                        "Barra",
                        "POSITIVE_PROGRESSION",
                        prescribedLoadKg = 72.5,
                        avgWeightKg = 70.0,
                        setCount = 4,
                    ),
                ),
                routineRequiresDeload = false,
            )

            val item = useCase(sessionId).exercises.single()

            assertEquals(1, item.pairs.size)
            assertEquals(
                ProgressionClassification.POSITIVE_PROGRESSION,
                item.classification,
            )
            // La señal del ejercicio coincide con la de su único par
            assertEquals(
                (item.pairs.single().signal as ActionSignal.IncreaseLoad).targetKg,
                (item.signal as ActionSignal.IncreaseLoad).targetKg,
                0.001,
            )
        }

    // ----- CA-40.04: el ejercicio muestra la lectura consolidada -----

    @Test
    fun `given one pair progressed, when reading the exercise, then the consolidated classification is progression`() =
        runTest {
            // La consolidación la resuelve el motor al cerrar; el resumen la lee tal cual.
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise(classification = "POSITIVE_PROGRESSION")),
                pairs = listOf(
                    pair(6L, "Mancuerna", "POSITIVE_PROGRESSION"),
                    pair(8L, "Polea", "REGRESSION"),
                ),
                routineRequiresDeload = false,
            )

            val item = useCase(sessionId).exercises.single()

            assertEquals(
                ProgressionClassification.POSITIVE_PROGRESSION,
                item.classification,
            )
        }

    @Test
    fun `given no pair rows, when building the summary, then the exercise line still resolves`() =
        runTest {
            // Una sesión de descarga no escribe clasificación por par; la fila del ejercicio
            // tiene que seguir siendo presentable.
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise(classification = null)),
                pairs = emptyList(),
                routineRequiresDeload = false,
            )

            val item = useCase(sessionId).exercises.single()

            assertTrue(item.pairs.isEmpty())
            assertTrue(item.signal is ActionSignal.FirstSession)
        }

    // ----- HU-46: la señal de rango 8–12 manda sobre la comparación con la sesión anterior -----

    private suspend fun singlePairSignal(pair: SessionPairSummaryDto, classification: String? = null): ActionSignal {
        coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
            info = info,
            exercises = listOf(exercise(classification = classification)),
            pairs = listOf(pair),
            routineRequiresDeload = false,
        )
        return useCase(sessionId).exercises.single().signal
    }

    @Test
    fun `given the first session at the top, when building the summary, then it is 1 of 4 and not a first session`() =
        runTest {
            // QA-01: «Mantener — 1 de 4» sin sesiones previas
            val signal = singlePairSignal(
                pair(6L, "Mancuerna", classification = null, rangeReading = "TOP", topRangeStreak = 1),
            )
            assertEquals(ActionSignal.TopOfRange(1), signal)
        }

    @Test
    fun `given a session below the range, when building the summary, then lower the weight`() = runTest {
        // QA-03
        val signal = singlePairSignal(
            pair(6L, "Mancuerna", "REGRESSION", rangeReading = "BELOW", topRangeStreak = 0),
            classification = "REGRESSION",
        )
        assertEquals(ActionSignal.DecreaseLoad, signal)
    }

    @Test
    fun `given the fourth top session, when building the summary, then increase to the prescribed load`() =
        runTest {
            // QA-04: «Subir carga → 60 kg + el incremento vigente»
            val signal = singlePairSignal(
                pair(
                    6L,
                    "Mancuerna",
                    "MAINTENANCE",
                    prescribedLoadKg = 62.5,
                    avgWeightKg = 60.0,
                    rangeReading = "TOP",
                    topRangeStreak = 4,
                ),
                classification = "MAINTENANCE",
            )
            assertEquals(ActionSignal.IncreaseLoad(62.5, 4), signal)
        }

    @Test
    fun `given a session within the range, when building the summary, then keep within the range`() = runTest {
        // QA-02
        val signal = singlePairSignal(
            pair(6L, "Mancuerna", "MAINTENANCE", rangeReading = "WITHIN", topRangeStreak = 0),
            classification = "MAINTENANCE",
        )
        assertEquals(ActionSignal.WithinRange, signal)
    }

    @Test
    fun `given a bodyweight pair that admits added weight, when four top sessions close, then add extra weight`() =
        runTest {
            // QA-26: Dominadas con Barra Fija, sin carga externa
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise().copy(isBodyweight = 1)),
                pairs = listOf(
                    pair(
                        5L,
                        "Barra Fija",
                        "MAINTENANCE",
                        rangeReading = "TOP",
                        topRangeStreak = 4,
                        admitsAddedWeight = true,
                    ),
                ),
                routineRequiresDeload = false,
            )
            assertEquals(ActionSignal.AddExtraWeight(4), useCase(sessionId).exercises.single().signal)
        }

    @Test
    fun `given a bodyweight pair without added weight, when building the summary, then no weight signal`() =
        runTest {
            // QA-27: Fondos en Banco no admite «Peso Añadido»: se conserva la lectura de siempre
            coEvery { repository.getSessionSummaryData(sessionId) } returns SessionSummaryData(
                info = info,
                exercises = listOf(exercise().copy(isBodyweight = 1)),
                pairs = listOf(
                    pair(9L, "Peso Corporal", "MAINTENANCE", rangeReading = "TOP", topRangeStreak = 4),
                ),
                routineRequiresDeload = false,
            )
            assertTrue(useCase(sessionId).exercises.single().signal is ActionSignal.BodyweightSignal)
        }
}
