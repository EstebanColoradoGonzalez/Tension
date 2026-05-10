package com.estebancoloradogonzalez.tension.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

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
    val alternativeSelectionState by viewModel.alternativeSelectionState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToSessionSummary.collect { sessionId ->
            onNavigateToSessionSummary(sessionId)
        }
    }

    BackHandler {
        viewModel.onCloseSessionRequested()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SessionTopBar(
                routineName = uiState.routineName,
                versionNumber = uiState.versionNumber,
                isDeloadSession = uiState.isDeloadSession,
                deloadProgress = uiState.deloadProgress,
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
                        isDeloadSession = uiState.isDeloadSession,
                        onRegister = { onNavigateToRegisterSet(exercise.sessionExerciseId) },
                        onSubstitute = { onNavigateToSubstitute(exercise.sessionExerciseId) },
                        onViewDetail = { exercise.exerciseId?.let { onNavigateToExerciseDetail(it) } },
                        onFinalize = { viewModel.onFinalizeExercise(exercise.sessionExerciseId) },
                        onSelectAlternative = { viewModel.onSelectAlternative(exercise) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { viewModel.onCloseSessionRequested() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isClosing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B4F4F),
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6B4F4F)),
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.session_close),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (uiState.showCloseDialog) {
            CloseSessionDialog(
                isAllCompleted = uiState.isAllCompleted,
                incompleteCount = uiState.incompleteCount,
                isClosing = uiState.isClosing,
                onConfirm = { viewModel.onCloseSessionConfirmed() },
                onDismiss = { viewModel.onCloseDialogDismissed() },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // Alternative selection bottom sheet
    if (alternativeSelectionState.isVisible) {
        AlternativeSelectionSheet(
            state = alternativeSelectionState,
            onDismiss = viewModel::onDismissAlternativeSelection,
            onAlternativeSelected = viewModel::onAlternativeSelected,
            onConfirm = viewModel::onConfirmAlternativeSelection,
        )
    }
}

@Composable
private fun SessionTopBar(
    routineName: String,
    versionNumber: Int,
    isDeloadSession: Boolean,
    deloadProgress: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.session_routine_version_format, routineName, versionNumber),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.session_active_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isDeloadSession) {
            Spacer(modifier = Modifier.height(4.dp))
            val badgeContainerColor = if (isSystemInDarkTheme()) {
                Color(0xFF1A2733)
            } else {
                Color(0xFFE3F2FD)
            }
            val semanticColors = LocalTensionSemanticColors.current
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = badgeContainerColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\uD83D\uDD04",
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.session_deload_badge, deloadProgress),
                        style = MaterialTheme.typography.labelMedium,
                        color = semanticColors.deloadActive,
                    )
                }
            }
        }
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
    isDeloadSession: Boolean,
    onRegister: () -> Unit,
    onSubstitute: () -> Unit,
    onViewDetail: () -> Unit,
    onFinalize: () -> Unit,
    onSelectAlternative: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    when {
        exercise.status == ExerciseSessionStatus.NOT_STARTED -> NotStartedExerciseRow(
            exercise = exercise,
            isDeloadSession = isDeloadSession,
            onRegister = onRegister,
            onSubstitute = onSubstitute,
            onViewDetail = onViewDetail,
            onSelectAlternative = onSelectAlternative,
        )
        exercise.status == ExerciseSessionStatus.IN_PROGRESS -> InProgressExerciseRow(
            exercise = exercise,
            isDeloadSession = isDeloadSession,
            isDark = isDark,
            onRegister = onRegister,
            onFinalize = onFinalize,
            onViewDetail = onViewDetail,
        )
        else -> CompletedExerciseRow(
            exercise = exercise,
            isDark = isDark,
            onRegister = onRegister,
            onViewDetail = onViewDetail,
        )
    }
}

@Composable
private fun NotStartedExerciseRow(
    exercise: ExerciseUiItem,
    isDeloadSession: Boolean,
    onRegister: () -> Unit,
    onSubstitute: () -> Unit,
    onViewDetail: () -> Unit,
    onSelectAlternative: () -> Unit,
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
                if (exercise.isBodyweight) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_outside_gym),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp,
                            ),
                        )
                    }
                }
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LoadText(exercise = exercise, isDeloadSession = isDeloadSession)

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
                    if (!exercise.hasAlternatives) {
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
                if (exercise.hasAlternatives) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onSelectAlternative,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = stringResource(R.string.select_exercise_for_slot),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp),
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
}

@Composable
private fun InProgressExerciseRow(
    exercise: ExerciseUiItem,
    isDeloadSession: Boolean,
    isDark: Boolean,
    onRegister: () -> Unit,
    onFinalize: () -> Unit,
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
                if (exercise.isBodyweight) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_outside_gym),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp,
                            ),
                        )
                    }
                }
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LoadText(exercise = exercise, isDeloadSession = isDeloadSession)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isExtraSet = exercise.completedSets >= exercise.sets
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = if (isExtraSet) {
                                stringResource(R.string.session_extra_set)
                            } else {
                                stringResource(R.string.session_register_set)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    FilledTonalButton(
                        onClick = onFinalize,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.session_finalize),
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
    onRegister: () -> Unit,
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
                if (exercise.isBodyweight) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_outside_gym),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp,
                            ),
                        )
                    }
                }
                Text(
                    text = exercise.statusDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRegister,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.session_extra_set),
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
private fun StatusIndicator(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = color, shape = CircleShape),
    )
}

@Composable
private fun LoadText(exercise: ExerciseUiItem, isDeloadSession: Boolean = false) {
    val isNoHistory = !exercise.isBodyweight && !exercise.isIsometric &&
        exercise.prescribedLoadKg == null
    val semanticColors = LocalTensionSemanticColors.current

    val textColor = if (isDeloadSession && !isNoHistory) {
        semanticColors.deloadActive
    } else if (isNoHistory || exercise.isBodyweight || exercise.isIsometric) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = exercise.loadDisplayText,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontStyle = if (isNoHistory) FontStyle.Italic else FontStyle.Normal,
        ),
        color = textColor,
    )
}

@Composable
private fun CloseSessionDialog(
    isAllCompleted: Boolean,
    incompleteCount: Int,
    isClosing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (!isAllCompleted) {
            {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            null
        },
        title = { Text(text = stringResource(R.string.session_close_title)) },
        text = {
            Text(
                text = if (isAllCompleted) {
                    stringResource(R.string.session_close_complete_message)
                } else {
                    stringResource(R.string.session_close_incomplete_message, incompleteCount)
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isClosing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                ),
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (isAllCompleted) {
                            stringResource(R.string.session_close_confirm_complete)
                        } else {
                            stringResource(R.string.session_close_confirm_incomplete)
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isClosing) {
                Text(text = stringResource(R.string.session_close_cancel))
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AlternativeSelectionSheet(
    state: AlternativeSelectionUiState,
    onDismiss: () -> Unit,
    onAlternativeSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.select_exercise_for_slot),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(state.alternatives) { option ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(option.name) },
                        supportingContent = { Text("${option.muscleZonesSummary} · ${option.equipmentTypeName}") },
                        modifier = Modifier.clickable { onAlternativeSelected(option.exerciseId) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = if (state.selectedExerciseId == option.exerciseId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    )
                }
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = onConfirm,
                enabled = state.selectedExerciseId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(stringResource(R.string.confirm_selection_button))
            }
        }
    }
}
