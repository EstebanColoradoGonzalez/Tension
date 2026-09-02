package com.estebancoloradogonzalez.tension.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import androidx.compose.material3.TextButton
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.ui.components.EntityNameText
import com.estebancoloradogonzalez.tension.ui.components.ReassignRoutineDialog
import com.estebancoloradogonzalez.tension.ui.components.weekDayName
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActiveSession: (Long) -> Unit,
    viewModel: SessionPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToActiveSession.collect { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    EntityNameText(
                        text = stringResource(
                            R.string.preview_title_format,
                            dayRoutineText(uiState.weekDay, uiState.routineName),
                            uiState.versionNumber,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.isDeloadActive) {
                item {
                    DeloadBanner(sessionsRemaining = uiState.deloadSessionsRemaining)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (uiState.isTemporaryOverride) {
                item {
                    Text(
                        text = stringResource(R.string.home_reassign_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            itemsIndexed(uiState.exercises) { index, exercise ->
                PreviewExerciseCard(exercise = exercise)
                if (index < uiState.exercises.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !uiState.isDayResolved,
                ) {
                    Text(text = stringResource(R.string.preview_start_session))
                }

                if (uiState.canReassign) {
                    TextButton(
                        onClick = {
                            if (uiState.isTemporaryOverride) {
                                viewModel.undoReassign()
                            } else {
                                viewModel.openReassignDialog()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = if (uiState.isTemporaryOverride) {
                                stringResource(R.string.home_reassign_undo)
                            } else {
                                stringResource(R.string.home_reassign_action)
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (uiState.isReassignDialogOpen) {
        ReassignRoutineDialog(
            options = uiState.reassignOptions,
            onConfirm = { routineId -> viewModel.confirmReassign(routineId) },
            onDismiss = { viewModel.dismissReassignDialog() },
        )
    }
}

/**
 * Compone `Dia - Rutina` para el titulo del preview. Sin dia conocido, el nombre de la
 * rutina se presenta solo.
 */
@Composable
private fun dayRoutineText(weekDay: WeekDay?, routineName: String): String {
    return if (weekDay == null) {
        routineName
    } else {
        stringResource(R.string.session_day_routine_format, weekDayName(weekDay), routineName)
    }
}

@Composable
private fun DeloadBanner(sessionsRemaining: Int) {
    val semanticColors = LocalTensionSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = semanticColors.deloadActive.copy(alpha = 0.12f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "\uD83D\uDD04", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.preview_deload_banner, sessionsRemaining),
                style = MaterialTheme.typography.bodyMedium,
                color = semanticColors.deloadActive,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PreviewExerciseCard(exercise: PreviewExerciseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntityNameText(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (exercise.showOutOfGymBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_outside_gym),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${exercise.muscleZones} · ${exercise.equipmentTypeName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = exercise.setsDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = exercise.repsDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = if (exercise.isRepsSpecial) FontStyle.Italic else FontStyle.Normal,
                    )
                }
                Text(
                    text = exercise.loadDisplayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
