package com.estebancoloradogonzalez.tension.ui.catalog.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Segmented three-option selector for the progression difficulty of an exercise.
 *
 * Follows the visual and accessibility pattern of the capture unit selector: minimum
 * touch target of 48 dp per option (RNF06) and an explicit content description.
 */
@Composable
fun ProgressionDifficultySelector(
    selectedDifficulty: ProgressionDifficulty,
    onDifficultySelected: (ProgressionDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row {
            DifficultyOption(
                difficulty = ProgressionDifficulty.LOW,
                label = stringResource(R.string.exercise_difficulty_low),
                description = stringResource(R.string.exercise_difficulty_low_description),
                isSelected = selectedDifficulty == ProgressionDifficulty.LOW,
                onDifficultySelected = onDifficultySelected,
            )
            DifficultyOption(
                difficulty = ProgressionDifficulty.MEDIUM,
                label = stringResource(R.string.exercise_difficulty_medium),
                description = stringResource(R.string.exercise_difficulty_medium_description),
                isSelected = selectedDifficulty == ProgressionDifficulty.MEDIUM,
                onDifficultySelected = onDifficultySelected,
            )
            DifficultyOption(
                difficulty = ProgressionDifficulty.HIGH,
                label = stringResource(R.string.exercise_difficulty_high),
                description = stringResource(R.string.exercise_difficulty_high_description),
                isSelected = selectedDifficulty == ProgressionDifficulty.HIGH,
                onDifficultySelected = onDifficultySelected,
            )
        }
    }
}

@Composable
private fun RowScope.DifficultyOption(
    difficulty: ProgressionDifficulty,
    label: String,
    description: String,
    isSelected: Boolean,
    onDifficultySelected: (ProgressionDifficulty) -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = isSelected) { onDifficultySelected(difficulty) }
            .semantics { contentDescription = description },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Box(
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
