package com.estebancoloradogonzalez.tension.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.components.EntityNameText
import com.estebancoloradogonzalez.tension.ui.components.ReassignRoutineDialog
import com.estebancoloradogonzalez.tension.ui.components.weekDayName
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.model.DayOutcome
import com.estebancoloradogonzalez.tension.domain.model.UpcomingSession
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

@Composable
fun HomeScreen(
    onNavigateToAlerts: () -> Unit,
    onNavigateToActiveSession: (Long) -> Unit,
    onNavigateToDeloadManagement: () -> Unit,
    onNavigateToPreview: (Long, String, Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
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
        viewModel.navigationEvent.collect { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        HomeTopBar(
            alertCount = uiState.alertCount,
            onAlertClick = onNavigateToAlerts,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.showResumeCard) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ResumeSessionCard(
                        routineName = uiState.activeSession?.routineName ?: "",
                        versionNumber = uiState.activeSession?.versionNumber ?: 0,
                        completedExercises = uiState.activeSession?.completedExercises ?: 0,
                        totalExercises = uiState.activeSession?.totalExercises ?: 0,
                        showSkipToday = uiState.showSkipToday,
                        canSkipToday = uiState.canSkipToday,
                        onResume = {
                            uiState.activeSession?.let { viewModel.resumeSession(it.sessionId) }
                        },
                        onSkipToday = { viewModel.skipToday() },
                    )
                }
            }

            if (uiState.showNextSessionCard) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    NextSessionCard(
                        weekDay = uiState.todaySession?.weekDay,
                        routineName = uiState.nextSession?.routineName ?: "",
                        versionNumber = uiState.nextSession?.versionNumber ?: 0,
                        isLoading = uiState.isLoading,
                        isTemporaryOverride = uiState.isTemporaryOverride,
                        canReassign = uiState.canReassign,
                        showSkipToday = uiState.showSkipToday,
                        canSkipToday = uiState.canSkipToday,
                        onStartSession = { viewModel.startSession() },
                        onReassign = { viewModel.openReassignDialog() },
                        onUndoReassign = { viewModel.undoReassign() },
                        onSkipToday = { viewModel.skipToday() },
                        onCardClick = {
                            uiState.nextSession?.let {
                                onNavigateToPreview(
                                    it.routineVersionId,
                                    it.routineName,
                                    it.versionNumber,
                                )
                            }
                        },
                    )
                }
            }

            if (uiState.showRestDayCard) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    RestDayCard(
                        weekDay = uiState.todaySession?.weekDay,
                        canReassign = uiState.canReassign,
                        onReassign = { viewModel.openReassignDialog() },
                    )
                }
            }

            if (uiState.showResolvedCard) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ResolvedDayCard(
                        outcome = uiState.dayOutcome,
                        upcoming = uiState.upcoming,
                        onUndoSkip = { viewModel.undoSkipToday() },
                    )
                }
            }

            if (uiState.deloadState != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    DeloadStatusCard(
                        deloadState = uiState.deloadState!!,
                        onNavigateToDeload = onNavigateToDeloadManagement,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                ProgressSection(microcycleCount = uiState.microcycleCount)
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
}

@Composable
private fun HomeTopBar(
    alertCount: Int,
    onAlertClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onAlertClick) {
            BadgedBox(
                badge = {
                    if (alertCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(alertCount.toString())
                        }
                    } else {
                        Badge(
                            modifier = Modifier.size(6.dp),
                            containerColor = MaterialTheme.colorScheme.outline,
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.home_alert_badge_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResumeSessionCard(
    routineName: String,
    versionNumber: Int,
    completedExercises: Int,
    totalExercises: Int,
    showSkipToday: Boolean,
    canSkipToday: Boolean,
    onResume: () -> Unit,
    onSkipToday: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_resume_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF410002),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            EntityNameText(
                text = stringResource(R.string.home_next_session_format, routineName, versionNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF410002),
            )

            Text(
                text = stringResource(
                    R.string.home_resume_progress,
                    completedExercises,
                    totalExercises,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF410002),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = stringResource(R.string.home_resume_session))
            }

            if (showSkipToday) {
                SkipTodayAction(
                    enabled = canSkipToday,
                    onSkipToday = onSkipToday,
                    contentColor = Color(0xFF410002),
                )
            }
        }
    }
}

/**
 * Cancelar el día. Deshabilitada en cuanto hay una serie registrada: entonces la salida es
 * reanudar la sesión y cerrarla como incompleta, y el texto de apoyo lo dice.
 */
@Composable
private fun SkipTodayAction(
    enabled: Boolean,
    onSkipToday: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onSkipToday,
        enabled = enabled,
        modifier = Modifier.height(48.dp),
    ) {
        Text(
            text = stringResource(R.string.home_skip_today),
            color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!enabled) {
        Text(
            text = stringResource(R.string.home_skip_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Sesion propuesta para hoy. Conserva forma, color y jerarquia de la tarjeta anterior: el
 * cambio de modelo no se traduce en un rediseno.
 *
 * El dia ya no viene embebido en el nombre de la rutina: se presenta como la relacion que
 * es. La accion de reasignacion va bajo el boton principal, discreta y fuera del flujo, y no
 * existe cuando hay una sesion en curso.
 */
@Composable
private fun NextSessionCard(
    weekDay: WeekDay?,
    routineName: String,
    versionNumber: Int,
    isLoading: Boolean,
    isTemporaryOverride: Boolean,
    canReassign: Boolean,
    showSkipToday: Boolean,
    canSkipToday: Boolean,
    onStartSession: () -> Unit,
    onReassign: () -> Unit,
    onUndoReassign: () -> Unit,
    onSkipToday: () -> Unit,
    onCardClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5DDDD),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_today_label),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C0E0E),
            )

            Spacer(modifier = Modifier.height(4.dp))

            EntityNameText(
                text = dayRoutineText(weekDay, routineName),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5C0E0E),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.version_label_format, versionNumber),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5C0E0E),
            )

            if (isTemporaryOverride) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_reassign_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C0E0E),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = stringResource(R.string.home_start_session))
            }

            if (canReassign) {
                TextButton(
                    onClick = if (isTemporaryOverride) onUndoReassign else onReassign,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = if (isTemporaryOverride) {
                            stringResource(R.string.home_reassign_undo)
                        } else {
                            stringResource(R.string.home_reassign_action)
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (showSkipToday) {
                SkipTodayAction(enabled = canSkipToday, onSkipToday = onSkipToday)
            }
        }
    }
}

/**
 * El día ya se resolvió: se entrenó o se declaró que no se entrena.
 *
 * Presenta la sesión del siguiente día **sin dejar iniciarla**. Es lo que impide ejecutar
 * varias sesiones el mismo día, y la razón de que el botón se muestre deshabilitado en lugar
 * de ausente: lo que hay que comunicar no es que no exista, sino cuándo estará disponible.
 */
@Composable
private fun ResolvedDayCard(
    outcome: DayOutcome?,
    upcoming: UpcomingSession?,
    onUndoSkip: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (outcome == DayOutcome.SKIPPED) {
                    stringResource(R.string.home_day_skipped_title)
                } else {
                    stringResource(R.string.home_day_trained_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (outcome == DayOutcome.SKIPPED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_day_skipped_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            if (upcoming == null) {
                Text(
                    text = stringResource(R.string.home_upcoming_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.home_upcoming_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                EntityNameText(
                    text = stringResource(
                        R.string.session_day_routine_format,
                        weekDayName(upcoming.weekDay),
                        upcoming.routineName,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                ) {
                    Text(text = stringResource(R.string.home_start_session))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.home_upcoming_available,
                        weekDayName(upcoming.weekDay),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (outcome == DayOutcome.SKIPPED) {
                TextButton(
                    onClick = onUndoSkip,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_skip_undo),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Dia de descanso: el dia no tiene rutina asignada. El descanso se presenta como un estado
 * del plan, no como pantalla vacia, y ofrece la misma reasignacion que el resto de los dias
 * (entrenar un domingo es posible sin que el domingo deje de ser dia sin rutina).
 */
@Composable
private fun RestDayCard(
    weekDay: WeekDay?,
    canReassign: Boolean,
    onReassign: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_rest_day_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    R.string.home_rest_day_body,
                    weekDay?.let { weekDayName(it) } ?: "",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (canReassign) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onReassign,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_rest_day_action),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Compone `Dia - Rutina`. Sin dia conocido, el nombre de la rutina se presenta solo: la
 * ausencia de dia no debe dejar un separador huerfano en pantalla.
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
private fun ProgressSection(microcycleCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = microcycleCount.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_microcycles_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeloadStatusCard(
    deloadState: DeloadHomeState,
    onNavigateToDeload: () -> Unit,
) {
    val semanticColors = LocalTensionSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (deloadState) {
                    is DeloadHomeState.Active -> {
                        Text(
                            text = "\uD83D\uDD04",
                            modifier = Modifier.size(24.dp),
                            color = semanticColors.deloadActive,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_deload_active),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    is DeloadHomeState.Required -> {
                        Text(
                            text = "⚠\uFE0F",
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.home_deload_required,
                                deloadState.routineName,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            if (deloadState is DeloadHomeState.Active) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.home_deload_progress,
                        deloadState.progress,
                        deloadState.totalSessions,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToDeload) {
                Text(
                    text = stringResource(R.string.home_deload_manage),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
