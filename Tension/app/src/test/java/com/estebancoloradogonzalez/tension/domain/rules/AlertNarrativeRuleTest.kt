package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.PlateauCause
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertNarrativeRuleTest {

    private val headlines = listOf(
        AlertNarrativeRule.plateauHeadline("Elevación Lateral", 10),
        AlertNarrativeRule.progressionRateHeadline("Elevación Lateral", 18, 6L),
        AlertNarrativeRule.rirHeadline("Push", 1.2, isLow = true, sessions = 3),
        AlertNarrativeRule.rirHeadline("Push", 1.9, isLow = false, sessions = 3),
        AlertNarrativeRule.adherenceHeadline(45, 2),
        AlertNarrativeRule.tonnageHeadline("Espalda", 18, 2),
        AlertNarrativeRule.inactivityHeadline("Pull", 16L),
        AlertNarrativeRule.deloadHeadline("Push"),
    )

    private val explanations = listOf(
        AlertNarrativeRule.plateauExplanation(
            "Elevación Lateral", 10, ProgressionDifficulty.HIGH, PlateauCause.LOW_RIR_LIMIT,
        ),
        AlertNarrativeRule.plateauExplanation(
            "Prensa Inclinada", 5, ProgressionDifficulty.LOW, PlateauCause.MIXED,
        ),
        AlertNarrativeRule.progressionRateExplanation(
            "Elevación Lateral", 18, ProgressionDifficulty.MEDIUM, isCritical = false,
        ),
        AlertNarrativeRule.progressionRateExplanation(
            "Elevación Lateral", 8, ProgressionDifficulty.HIGH, isCritical = true,
        ),
        AlertNarrativeRule.rirExplanation("Push", 1.2, isLow = true),
        AlertNarrativeRule.rirExplanation("Push", 1.9, isLow = false),
        AlertNarrativeRule.adherenceExplanation(45, 2),
        AlertNarrativeRule.adherenceExplanation(30, 3),
        AlertNarrativeRule.tonnageExplanation("Espalda", 18, isDeload = false),
        AlertNarrativeRule.tonnageExplanation("Espalda", 18, isDeload = true),
        AlertNarrativeRule.inactivityExplanation("Pull", 16L, listOf("Espalda", "Bíceps")),
        AlertNarrativeRule.inactivityExplanation("Pull", 16L, emptyList()),
        AlertNarrativeRule.deloadExplanation("Push", 60),
    )

    private val actionTexts = SuggestedActionKind.entries.map { kind ->
        AlertNarrativeRule.suggestedActionText(
            kind = kind,
            exerciseName = "Elevación Lateral",
            routineName = "Push",
            incrementKg = 2.5,
        )
    }

    private val allTexts = headlines + explanations + actionTexts

    @Test
    fun `given any alert text, when it is produced, then it exposes no internal identifiers`() {
        listOf("_id", "id=", "exerciseId", "routineId").forEach { forbidden ->
            allTexts.forEach { text ->
                assertFalse("\"$forbidden\" leaked into: $text", text.contains(forbidden))
            }
        }
    }

    @Test
    fun `given any alert text, when it is produced, then it exposes no internal codes`() {
        listOf(
            "PLATEAU",
            "LOW_PROGRESSION_RATE",
            "RIR_OUT_OF_RANGE",
            "LOW_ADHERENCE",
            "TONNAGE_DROP",
            "ROUTINE_INACTIVITY",
            "ROUTINE_REQUIRES_DELOAD",
            "MEDIUM_ALERT",
            "HIGH_ALERT",
            "CRISIS",
        ).forEach { forbidden ->
            allTexts.forEach { text ->
                assertFalse("\"$forbidden\" leaked into: $text", text.contains(forbidden))
            }
        }
    }

    @Test
    fun `given any alert text, when it is produced, then it names no rule of the engine`() {
        listOf("Rule", "Threshold", "umbral").forEach { forbidden ->
            allTexts.forEach { text ->
                assertFalse("\"$forbidden\" leaked into: $text", text.contains(forbidden))
            }
        }
    }

    @Test
    fun `given a headline, when it is produced, then it names the element it is about`() {
        assertTrue(headlines[0].contains("Elevación Lateral"))
        assertTrue(headlines[1].contains("Elevación Lateral"))
        assertTrue(headlines[2].contains("Push"))
        assertTrue(headlines[3].contains("Push"))
        assertTrue(headlines[4].contains("semanas"))
        assertTrue(headlines[5].contains("Espalda"))
        assertTrue(headlines[6].contains("Pull"))
        assertTrue(headlines[7].contains("Push"))
    }

    @Test
    fun `given a headline, when it is produced, then it carries the figure that originated it`() {
        assertTrue(headlines[0].contains("10"))
        assertTrue(headlines[1].contains("18"))
        assertTrue(headlines[2].contains("3"))
        assertTrue(headlines[4].contains("45"))
        assertTrue(headlines[5].contains("18"))
        assertTrue(headlines[6].contains("16"))
    }

    @Test
    fun `given a hard to progress exercise, when the plateau is explained, then the wait is justified`() {
        val text = AlertNarrativeRule.plateauExplanation(
            "Elevación Lateral", 10, ProgressionDifficulty.HIGH, PlateauCause.MIXED,
        )

        assertTrue(text.contains("despacio"))
        assertTrue(text.contains("10"))
    }

    @Test
    fun `given a planned deload, when the tonnage drop is explained, then it is not a regression`() {
        val text = AlertNarrativeRule.tonnageExplanation("Espalda", 30, isDeload = true)

        assertTrue(text.contains("descarga planificada"))
    }

    @Test
    fun `given every suggested action, when it is written, then none of them is empty`() {
        assertTrue(actionTexts.size == SuggestedActionKind.entries.size)
        actionTexts.forEach { text ->
            assertTrue("an action was left without text", text.isNotBlank())
        }
    }

    @Test
    fun `given every alert text, when it is produced, then none of them is blank`() {
        allTexts.forEach { text ->
            assertTrue("a blank alert text was produced", text.isNotBlank())
        }
    }
}
