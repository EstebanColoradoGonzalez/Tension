package com.estebancoloradogonzalez.tension.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus

@Composable
fun ActiveSessionScreen(
    onNavigateToRegisterSet: (Long) -> Unit,
    onNavigateToSubstitute: (Long) -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit,
    onNavigateToSessionSummary: (Long) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { /* no-op: user must close session formally */ }

    Column(modifier = Modifier.fillMaxSize()) {
        SessionTopBar(
            moduleCode = uiState.moduleCode,
            versionNumber = uiState.versionNumber,
        )

        ProgressBar(
            completedCount = uiState.completedCount,
            totalCount = uiState.totalCount,
            progress = uiState.progress,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(uiState.exercises, key = { it.sessionExerciseId }) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onRegister = { onNavigateToRegisterSet(exercise.sessionExerciseId) },
                    onSubstitute = { onNavigateToSubstitute(exercise.sessionExerciseId) },
                    onViewDetail = { onNavigateToExerciseDetail(exercise.exerciseId) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { /* TODO: HU-09 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B4F4F),
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6B4F4F)),
                    ),
                ) {
                    Text(text = stringResource(R.string.session_close))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SessionTopBar(
    moduleCode: String,
    versionNumber: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.session_module_version_format, moduleCode, versionNumber),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.session_active_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressBar(
    completedCount: Int,
    totalCount: Int,
    progress: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.session_progress_format, completedCount, totalCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseUiItem,
    onRegister: () -> Unit,
    onSubstitute: () -> Unit,
    onViewDetail: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    when (exercise.status) {
        ExerciseSessionStatus.NOT_STARTED -> NotStartedExerciseRow(
            exercise = exercise,
            onRegister = onRegister,
            onSubstitute = onSubstitute,
            onViewDetail = onViewDetail,
        )
        ExerciseSessionStatus.IN_PROGRESS -> InProgressExerciseRow(
            exercise = exercise,
            isDark = isDark,
            onRegister = onRegister,
            onViewDetail = onViewDetail,
        )
        ExerciseSessionStatus.COMPLETED -> CompletedExerciseRow(
            exercise = exercise,
            isDark = isDark,
            onViewDetail = onViewDetail,
        )
    }
}

@Composable
private fun NotStartedExerciseRow(
    exercise: ExerciseUiItem,
    onRegister: () -> Unit,
    onSubstitute: () -> Unit,
    onViewDetail: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StatusIndicator(color = Color(0xFF857370))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LoadText(exercise = exercise)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onRegister,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.session_register_set),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    OutlinedButton(
                        onClick = onSubstitute,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.session_substitute),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(
                        onClick = onViewDetail,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InProgressExerciseRow(
    exercise: ExerciseUiItem,
    isDark: Boolean,
    onRegister: () -> Unit,
    onViewDetail: () -> Unit,
) {
    val bgColor = if (isDark) Color(0xFF1A2733) else Color(0xFFE3F2FD)
    val indicatorColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StatusIndicator(color = indicatorColor)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LoadText(exercise = exercise)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.session_register_set),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(
                        onClick = onViewDetail,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedExerciseRow(
    exercise: ExerciseUiItem,
    isDark: Boolean,
    onViewDetail: () -> Unit,
) {
    val bgColor = if (isDark) Color(0xFF1A2E1A) else Color(0xFFE8F5E9)
    val indicatorColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .alpha(0.7f),
            verticalAlignment = Alignment.Top,
        ) {
            StatusIndicator(color = indicatorColor)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onViewDetail,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = color, shape = CircleShape),
    )
}

@Composable
private fun LoadText(exercise: ExerciseUiItem) {
    val isNoHistory = !exercise.isBodyweight && !exercise.isIsometric &&
        exercise.prescribedLoadKg == null

    Text(
        text = exercise.loadDisplayText,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontStyle = if (isNoHistory) FontStyle.Italic else FontStyle.Normal,
        ),
        color = if (isNoHistory || exercise.isBodyweight || exercise.isIsometric) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}
