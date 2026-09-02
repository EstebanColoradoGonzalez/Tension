package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.SuggestedAction
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionTarget

private val MIN_TOUCH_TARGET = 48.dp

/**
 * The block that turns an alert into a decision: what the executant can do about it and,
 * when the app can take them there, a shortcut that does it. Without a destination the
 * block is text only — resting more or reviewing technique is not something to navigate
 * to.
 */
@Composable
fun SuggestedActionCard(
    action: SuggestedAction,
    onNavigateToExerciseHistory: (Long) -> Unit,
    onNavigateToDeloadManagement: () -> Unit,
    onNavigateToTrainingPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.alert_detail_suggested_action_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = action.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (val target = action.target) {
                null -> Unit
                is SuggestedActionTarget.ExerciseHistory -> ShortcutButton(
                    labelRes = R.string.alert_action_go_to_exercise,
                    onClick = { onNavigateToExerciseHistory(target.exerciseId) },
                )
                SuggestedActionTarget.DeloadManagement -> ShortcutButton(
                    labelRes = R.string.alert_action_manage_deload,
                    onClick = onNavigateToDeloadManagement,
                )
                SuggestedActionTarget.TrainingPlan -> ShortcutButton(
                    labelRes = R.string.alert_action_go_to_plan,
                    onClick = onNavigateToTrainingPlan,
                )
            }
        }
    }
}

@Composable
private fun ShortcutButton(labelRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = MIN_TOUCH_TARGET),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(text = stringResource(labelRes))
    }
}
