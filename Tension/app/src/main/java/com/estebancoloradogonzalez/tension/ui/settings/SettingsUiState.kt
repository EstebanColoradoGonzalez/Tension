package com.estebancoloradogonzalez.tension.ui.settings

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule

data class SettingsUiState(
    val isLoading: Boolean = true,
    val baseThreshold: Int = PlateauThresholdRule.DEFAULT_BASE_THRESHOLD,
    val rangeError: String? = null,
) {
    val canDecreaseThreshold: Boolean
        get() = baseThreshold > PlateauThresholdRule.MIN_BASE_THRESHOLD

    val canIncreaseThreshold: Boolean
        get() = baseThreshold < PlateauThresholdRule.MAX_BASE_THRESHOLD

    val lowThresholdSessions: Int
        get() = effectiveThreshold(ProgressionDifficulty.LOW)

    val mediumThresholdSessions: Int
        get() = effectiveThreshold(ProgressionDifficulty.MEDIUM)

    val highThresholdSessions: Int
        get() = effectiveThreshold(ProgressionDifficulty.HIGH)

    private fun effectiveThreshold(difficulty: ProgressionDifficulty): Int =
        PlateauThresholdRule.effectiveThreshold(baseThreshold, difficulty)
}
