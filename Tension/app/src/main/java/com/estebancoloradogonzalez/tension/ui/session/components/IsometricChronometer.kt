package com.estebancoloradogonzalez.tension.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.session.TimerState
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

@Composable
fun IsometricChronometer(
    timerState: TimerState,
    timerSeconds: Int,
    minSeconds: Int?,
    maxSeconds: Int?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalTensionSemanticColors.current
    val effectiveMin = minSeconds ?: 0
    val effectiveMax = maxSeconds ?: 60

    val isInRange = timerSeconds >= effectiveMin
    val progressColor = if (isInRange) {
        semanticColors.progressionPositive
    } else {
        MaterialTheme.colorScheme.error
    }

    val progress = if (effectiveMax > 0) {
        (timerSeconds.toFloat() / effectiveMax.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val statusText = when {
        timerState == TimerState.IDLE -> ""
        timerSeconds < effectiveMin -> stringResource(R.string.chronometer_below_min)
        else -> stringResource(R.string.chronometer_in_range)
    }

    val statusIcon = when {
        timerState == TimerState.IDLE -> ""
        timerSeconds < effectiveMin -> "⚠\uFE0F"
        else -> "✅"
    }

    val accessibilityText = when (timerState) {
        TimerState.IDLE -> stringResource(R.string.chronometer_accessibility_idle)
        TimerState.RUNNING -> stringResource(
            R.string.chronometer_accessibility_running,
            timerSeconds,
        )
        TimerState.STOPPED -> stringResource(
            R.string.chronometer_accessibility_stopped,
            timerSeconds,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityText },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(160.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 8.dp,
            )
            Text(
                text = "${timerSeconds}s",
                style = MaterialTheme.typography.headlineLarge,
                color = progressColor,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.chronometer_range_label, effectiveMin, effectiveMax),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (statusText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$statusIcon $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = progressColor,
            )
        }

        if (timerState == TimerState.STOPPED && timerSeconds < effectiveMin) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.chronometer_below_range_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (timerState) {
                TimerState.IDLE -> {
                    Button(
                        onClick = onStartTimer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.chronometer_start))
                    }
                }
                TimerState.RUNNING -> {
                    Button(
                        onClick = onStopTimer,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(text = stringResource(R.string.chronometer_stop))
                    }
                }
                TimerState.STOPPED -> {
                    TextButton(
                        onClick = onResetTimer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.chronometer_reset))
                    }
                }
            }
        }
    }
}
