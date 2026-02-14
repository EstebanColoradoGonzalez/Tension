package com.estebancoloradogonzalez.tension.ui.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@Composable
fun HomeScreen(
    onNavigateToAlerts: () -> Unit,
    onNavigateToActiveSession: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                        moduleCode = uiState.activeSession?.moduleCode ?: "",
                        versionNumber = uiState.activeSession?.versionNumber ?: 0,
                        completedExercises = uiState.activeSession?.completedExercises ?: 0,
                        totalExercises = uiState.activeSession?.totalExercises ?: 0,
                        onResume = {
                            uiState.activeSession?.let { viewModel.resumeSession(it.sessionId) }
                        },
                    )
                }
            }

            if (uiState.showNextSessionCard) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    NextSessionCard(
                        moduleCode = uiState.nextSession?.moduleCode ?: "",
                        versionNumber = uiState.nextSession?.versionNumber ?: 0,
                        isLoading = uiState.isLoading,
                        onStartSession = { viewModel.startSession() },
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
    moduleCode: String,
    versionNumber: Int,
    completedExercises: Int,
    totalExercises: Int,
    onResume: () -> Unit,
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

            Text(
                text = stringResource(R.string.home_next_session_format, moduleCode, versionNumber),
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
        }
    }
}

@Composable
private fun NextSessionCard(
    moduleCode: String,
    versionNumber: Int,
    isLoading: Boolean,
    onStartSession: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5DDDD),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_next_session_format, moduleCode, versionNumber),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5C0E0E),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.home_next_session_label),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C0E0E),
            )

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
        }
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
