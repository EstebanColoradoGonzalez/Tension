package com.estebancoloradogonzalez.tension.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryEntry
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.ui.components.ProgressionIndicator
import com.estebancoloradogonzalez.tension.ui.components.TrendChartComposable
import com.estebancoloradogonzalez.tension.ui.components.TrendPoint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit = {},
    viewModel: ExerciseHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is ExerciseHistoryUiState.Loaded -> {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.data.exerciseName,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = stringResource(R.string.exercise_history_subtitle),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.navigate_back),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
                else -> {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.exercise_history_title),
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
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is ExerciseHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ExerciseHistoryUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.exercise_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is ExerciseHistoryUiState.Loaded -> {
                ExerciseHistoryContent(
                    data = state.data,
                    trendPoints = state.trendPoints,
                    yAxisLabel = state.yAxisLabel,
                    exerciseId = viewModel.exerciseId,
                    onNavigateToExerciseDetail = onNavigateToExerciseDetail,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ExerciseHistoryContent(
    data: ExerciseHistoryData,
    trendPoints: List<TrendPoint>,
    yAxisLabel: String,
    exerciseId: Long,
    onNavigateToExerciseDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        // Progression status badge
        item {
            ProgressionStatusBadge(
                status = data.progressionStatus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Trend chart
        item {
            TrendChartComposable(
                data = trendPoints,
                yAxisLabel = yAxisLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // Session entries list
        items(data.entries) { entry ->
            ExerciseHistoryEntryRow(
                entry = entry,
                isBodyweight = data.isBodyweight,
                isIsometric = data.isIsometric,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // "Ver técnica de ejecución" link
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(
                onClick = { onNavigateToExerciseDetail(exerciseId) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.exercise_history_view_technique),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProgressionStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (label, containerColor, labelColor) = when (status) {
        "IN_PROGRESSION", "MASTERED" -> Triple(
            stringResource(R.string.exercise_history_status_in_progression),
            Color(0xFFE8F5E9),
            Color(0xFF1A2E1A),
        )
        "IN_PLATEAU" -> Triple(
            stringResource(R.string.exercise_history_status_in_plateau),
            Color(0xFFFFF8E1),
            Color(0xFF4A3800),
        )
        "IN_DELOAD" -> Triple(
            stringResource(R.string.exercise_history_status_in_deload),
            Color(0xFFE3F2FD),
            Color(0xFF1A2733),
        )
        else -> Triple(
            stringResource(R.string.exercise_history_status_no_history),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ExerciseHistoryEntryRow(
    entry: ExerciseHistoryEntry,
    isBodyweight: Boolean,
    isIsometric: Boolean,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))
    val formattedDate = try {
        LocalDate.parse(entry.date).format(dateFormatter)
    } catch (_: Exception) {
        entry.date
    }

    val numberFormat = NumberFormat.getNumberInstance(Locale("es")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }

    val headlineText = when {
        isIsometric -> stringResource(
            R.string.exercise_history_isometric_format,
            entry.totalReps,
            numberFormat.format(entry.avgRir),
        )
        isBodyweight -> stringResource(
            R.string.exercise_history_bodyweight_format,
            entry.totalReps,
            numberFormat.format(entry.avgRir),
        )
        else -> stringResource(
            R.string.exercise_history_weight_format,
            numberFormat.format(entry.avgWeightKg),
            entry.totalReps,
            numberFormat.format(entry.avgRir),
        )
    }

    ListItem(
        overlineContent = {
            Text(
                text = stringResource(
                    R.string.exercise_history_session_format,
                    formattedDate,
                    entry.moduleCode,
                    entry.versionNumber,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                ProgressionIndicator(classification = entry.classification)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = classificationLabel(entry.classification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun classificationLabel(classification: ProgressionClassification?): String {
    return when (classification) {
        ProgressionClassification.POSITIVE_PROGRESSION -> "Progresión positiva"
        ProgressionClassification.MAINTENANCE -> "Mantenimiento"
        ProgressionClassification.REGRESSION -> "Regresión"
        null -> "Primera sesión"
    }
}
