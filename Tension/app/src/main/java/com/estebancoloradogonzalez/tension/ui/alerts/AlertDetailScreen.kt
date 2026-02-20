package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.model.AlertTriggerData
import com.estebancoloradogonzalez.tension.ui.components.AlertLevelIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    onNavigateToDeloadManagement: () -> Unit,
    viewModel: AlertDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.alert_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AlertDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is AlertDetailUiState.Loaded -> {
                AlertDetailContent(
                    detail = state.detail,
                    onNavigateToExerciseHistory = onNavigateToExerciseHistory,
                    onNavigateToDeloadManagement = onNavigateToDeloadManagement,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun AlertDetailContent(
    detail: AlertDetail,
    onNavigateToExerciseHistory: (Long) -> Unit,
    onNavigateToDeloadManagement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val levelContainerColor = when (detail.level) {
        "CRISIS" -> if (isDark) Color(0xFF930009) else Color(0xFFFFDAD6)
        "HIGH_ALERT" -> if (isDark) Color(0xFF4E2600) else Color(0xFFFFF3E0)
        "MEDIUM_ALERT" -> if (isDark) Color(0xFF4A3800) else Color(0xFFFFF8E1)
        else -> MaterialTheme.colorScheme.surface
    }
    val levelContentColor = when (detail.level) {
        "CRISIS" -> if (isDark) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.onErrorContainer
        else -> if (isDark) Color(0xFFE8E0DC) else Color(0xFF1C1B1B)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Card tipo y nivel
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = levelContainerColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlertLevelIndicator(level = detail.level, size = 16.dp)
                    Column {
                        Text(
                            text = alertTypeDisplayName(detail.type),
                            style = MaterialTheme.typography.titleMedium,
                            color = levelContentColor,
                        )
                        Text(
                            text = detail.entityName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = levelContentColor,
                        )
                        Text(
                            text = detail.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = levelContentColor,
                        )
                    }
                }
            }
        }

        item { HorizontalDivider() }

        // Datos que dispararon la alerta
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TriggerDataContent(triggerData = detail.triggerData)
                }
            }
        }

        item { HorizontalDivider() }

        // Análisis causal
        item {
            Text(
                text = detail.causalAnalysis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        if (detail.recommendations.isNotEmpty()) {
            item { HorizontalDivider() }

            item {
                Text(
                    text = stringResource(R.string.alert_detail_recommendations_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            items(detail.recommendations) { recommendation ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = "▸",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (detail.showExerciseHistoryLink || detail.showDeloadLink) {
            item { HorizontalDivider() }
        }

        if (detail.showExerciseHistoryLink && detail.exerciseId != null) {
            item {
                TextButton(
                    onClick = { onNavigateToExerciseHistory(detail.exerciseId) },
                ) {
                    Text(
                        text = stringResource(R.string.alert_detail_view_history),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (detail.showDeloadLink) {
            item {
                TextButton(
                    onClick = { onNavigateToDeloadManagement() },
                ) {
                    Text(
                        text = stringResource(R.string.alert_detail_manage_deload),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerDataContent(triggerData: AlertTriggerData) {
    when (triggerData) {
        is AlertTriggerData.PlateauTrigger -> {
            triggerData.sessions.forEachIndexed { index, session ->
                Text(
                    text = "Sesión ${index + 1}: ${session.weightKg} Kg · ${session.totalReps} reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (triggerData.sessions.isNotEmpty()) {
                Text(
                    text = "Período: ${triggerData.sessions.lastOrNull()?.date ?: ""} — ${triggerData.sessions.firstOrNull()?.date ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is AlertTriggerData.ProgressionRateTrigger -> {
            Text(
                text = "Tasa de progresión: ${triggerData.rate.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = triggerData.exerciseName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is AlertTriggerData.RirTrigger -> {
            Text(
                text = "RIR promedio: ${triggerData.avgRir} — Módulo ${triggerData.moduleCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        is AlertTriggerData.AdherenceTrigger -> {
            Text(
                text = "Adherencia: ${triggerData.percentage.toInt()}% (${triggerData.completedSessions}/${triggerData.plannedSessions} sesiones)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (triggerData.consecutiveWeeks > 1) {
                Text(
                    text = "${triggerData.consecutiveWeeks} semanas consecutivas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        is AlertTriggerData.TonnageDropTrigger -> {
            Text(
                text = "Grupo: ${triggerData.muscleGroup} — Caída: ${triggerData.dropPercentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tonelaje anterior: ${triggerData.previousTonnage.toInt()} Kg → actual: ${triggerData.currentTonnage.toInt()} Kg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (triggerData.isDeloadContextualized) {
                Text(
                    text = "Descarga planificada — caída esperada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1565C0),
                )
            }
        }
        is AlertTriggerData.InactivityTrigger -> {
            Text(
                text = "Módulo ${triggerData.moduleCode}: ${triggerData.daysSinceLastSession} días sin sesión",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (triggerData.muscleGroups.isNotEmpty()) {
                Text(
                    text = "Grupos afectados: ${triggerData.muscleGroups.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun alertTypeDisplayName(type: String): String = when (type) {
    "PLATEAU" -> "Meseta detectada"
    "LOW_PROGRESSION_RATE" -> "Tasa de progresión baja"
    "RIR_OUT_OF_RANGE" -> "RIR fuera de rango"
    "LOW_ADHERENCE" -> "Adherencia baja"
    "TONNAGE_DROP" -> "Caída de tonelaje"
    "MODULE_INACTIVITY" -> "Inactividad por módulo"
    "MODULE_REQUIRES_DELOAD" -> "Módulo requiere descarga"
    else -> type
}
