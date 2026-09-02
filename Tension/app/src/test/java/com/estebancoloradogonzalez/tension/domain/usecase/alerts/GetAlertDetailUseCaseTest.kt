package com.estebancoloradogonzalez.tension.domain.usecase.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.model.AlertTriggerData
import com.estebancoloradogonzalez.tension.domain.model.SuggestedAction
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionKind
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionTarget
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAlertDetailUseCaseTest {

    private val repository: AlertRepository = mockk()
    private val useCase = GetAlertDetailUseCase(repository)

    @Test
    fun `invoke returns alert detail from repository`() = runTest {
        val detail = AlertDetail(
            alertId = 1L,
            type = "PLATEAU",
            level = "HIGH_ALERT",
            entityName = "Press banca",
            message = "3 sesiones sin progresión",
            createdAt = "2026-02-15",
            triggerData = AlertTriggerData.PlateauTrigger(emptyList()),
            causalAnalysis = "Análisis causal",
            suggestedAction = SuggestedAction(
                kind = SuggestedActionKind.EXTEND_REPS_BEFORE_LOAD,
                text = "Añade una repetición por serie antes de subir peso",
                target = SuggestedActionTarget.ExerciseHistory(5L),
            ),
            exerciseId = 5L,
        )
        coEvery { repository.getAlertDetail(1L) } returns detail

        val result = useCase(1L)

        assertEquals(1L, result.alertId)
        assertEquals("PLATEAU", result.type)
        assertEquals(SuggestedActionTarget.ExerciseHistory(5L), result.suggestedAction.target)
    }
}
