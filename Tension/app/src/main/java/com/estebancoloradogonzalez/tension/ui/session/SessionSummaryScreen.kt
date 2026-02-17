package com.estebancoloradogonzalez.tension.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ActionSignal
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSummaryItem
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionSummary
import com.estebancoloradogonzalez.tension.ui.components.ProgressionIndicator
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: SessionSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.session_summary_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (uiState is SessionSummaryUiState.Success) {
                            val summary = (uiState as SessionSummaryUiState.Success).summary
                            Text(
                                text = stringResource(
                                    R.string.session_module_version_format,
                                    summary.moduleCode,
                                    summary.versionNumber,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is SessionSummaryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is SessionSummaryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is SessionSummaryUiState.Success -> {
                SessionSummaryContent(
                    summary = state.summary,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToExerciseHistory = onNavigateToExerciseHistory,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun SessionSummaryContent(
    summary: SessionSummary,
    onNavigateToHome: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tonnageFormatter = NumberFormat.getIntegerInstance(Locale("es", "ES"))
    val isCompleted = summary.status == "COMPLETED"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        // 1. Status + Tonnage Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isCompleted) {
                            stringResource(R.string.session_summary_completed)
                        } else {
                            stringResource(R.string.session_summary_incomplete)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${tonnageFormatter.format(summary.totalTonnageKg.toLong())} Kg",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.session_summary_exercises_completed,
                            summary.completedExercises,
                            summary.totalExercises,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }

        // 2. Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // 3. Exercise list
        items(summary.exercises) { item ->
            ExerciseSummaryRow(
                item = item,
                onClick = { onNavigateToExerciseHistory(item.exerciseId) },
            )
        }

        // 4. Spacer + Button
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(text = stringResource(R.string.session_summary_go_home))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExerciseSummaryRow(
    item: ExerciseSummaryItem,
    onClick: () -> Unit,
) {
    val semanticColors = LocalTensionSemanticColors.current

    ListItem(
        modifier = Modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        leadingContent = {
            ProgressionIndicator(classification = item.classification)
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (item.isMastered) Modifier.weight(1f, fill = false) else Modifier,
                )
                if (item.isMastered) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = stringResource(R.string.session_summary_mastered),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    )
                }
            }
        },
        supportingContent = {
            Column {
                // Line 1: Classification + context
                val classificationText = formatClassificationLine(item)
                if (classificationText != null) {
                    Text(
                        text = classificationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (item.classification) {
                            ProgressionClassification.POSITIVE_PROGRESSION ->
                                semanticColors.progressionPositive
                            ProgressionClassification.MAINTENANCE ->
                                semanticColors.maintenance
                            ProgressionClassification.REGRESSION ->
                                semanticColors.regression
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Line 2: Action signal
                Text(
                    text = formatActionSignal(item.signal, item.classification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

private fun formatClassificationLine(item: ExerciseSummaryItem): String? {
    val classification = item.classification ?: return null
    val classText = when (classification) {
        ProgressionClassification.POSITIVE_PROGRESSION -> "Progresión positiva"
        ProgressionClassification.MAINTENANCE -> "Mantenimiento"
        ProgressionClassification.REGRESSION -> "Regresión"
    }
    val context = when {
        item.isBodyweight -> "Peso corporal"
        item.isIsometric -> "Isométrico"
        else -> "%.1f Kg".format(item.weightKg)
    }
    return "$classText · $context"
}

private fun formatActionSignal(
    signal: ActionSignal,
    classification: ProgressionClassification?,
): String {
    return when (signal) {
        is ActionSignal.IncreaseLoad ->
            "Subir carga → %.1f Kg".format(signal.targetKg)
        is ActionSignal.ProgressInReps ->
            "Progresar en reps (misma carga)"
        is ActionSignal.MaintainLoad ->
            signal.message
        is ActionSignal.ConsiderDeload ->
            "Considerar descarga"
        is ActionSignal.BodyweightSignal -> {
            val diffText = when {
                signal.diff == null -> ""
                signal.diff > 0 -> " (+${signal.diff})"
                signal.diff == 0 -> " (=)"
                else -> " (${signal.diff})"
            }
            "${signal.totalReps} reps totales$diffText"
        }
        is ActionSignal.IsometricSignal -> {
            val statusText = when (classification) {
                ProgressionClassification.POSITIVE_PROGRESSION -> "Progresando"
                ProgressionClassification.MAINTENANCE -> "Manteniendo"
                ProgressionClassification.REGRESSION -> "Regresando"
                null -> ""
            }
            "${signal.setCount}×${signal.avgSeconds}s — $statusText"
        }
        is ActionSignal.IsometricMastered ->
            "4×45s — \uD83C\uDFC6 Dominado"
        is ActionSignal.FirstSession ->
            "Primera sesión — sin referencia"
    }
}
