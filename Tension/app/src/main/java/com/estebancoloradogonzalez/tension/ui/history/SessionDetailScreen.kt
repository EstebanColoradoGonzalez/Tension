package com.estebancoloradogonzalez.tension.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionDetailExercise
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.ui.components.ProgressionIndicator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is SessionDetailUiState.Loaded -> {
                    val detail = state.detail
                    val dateFormatter =
                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))
                    val formattedDate = try {
                        LocalDate.parse(detail.date).format(dateFormatter)
                    } catch (_: Exception) {
                        detail.date
                    }
                    val isCompleted = detail.status == "COMPLETED"
                    val statusLabel = if (isCompleted) {
                        stringResource(R.string.session_history_completed)
                    } else {
                        stringResource(R.string.session_history_incomplete)
                    }

                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(
                                        R.string.session_module_version_format,
                                        detail.moduleCode,
                                        detail.versionNumber,
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = "$formattedDate \u00b7 $statusLabel",
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
                                text = stringResource(R.string.session_history_title),
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
            is SessionDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is SessionDetailUiState.Loaded -> {
                SessionDetailContent(
                    detail = state.detail,
                    onExerciseClick = onNavigateToExerciseHistory,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    detail: SessionDetail,
    onExerciseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("es")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    val isCompleted = detail.status == "COMPLETED"
    val statusColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)

    LazyColumn(modifier = modifier) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(
                            R.string.session_detail_tonnage,
                            numberFormat.format(detail.totalTonnageKg),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.session_detail_exercises,
                            detail.completedExercises,
                            detail.totalExercises,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isCompleted) {
                            "\u2705 ${stringResource(R.string.session_history_completed)}"
                        } else {
                            "\u26A0\uFE0F ${stringResource(R.string.session_history_incomplete)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                    )
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

        items(detail.exercises) { exercise ->
            ExerciseDetailCard(
                exercise = exercise,
                onClick = { onExerciseClick(exercise.exerciseId) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun ExerciseDetailCard(
    exercise: SessionDetailExercise,
    onClick: () -> Unit,
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("es")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = exercise.exerciseName,
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressionIndicator(classification = exercise.classification)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = classificationLabel(exercise.classification),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (exercise.originalExerciseName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.session_detail_substituted,
                    exercise.originalExerciseName,
                ),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        exercise.sets.forEachIndexed { index, set ->
            SetRow(setNumber = index + 1, set = set, numberFormat = numberFormat)
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: SetData,
    numberFormat: NumberFormat,
) {
    Text(
        text = stringResource(
            R.string.session_detail_set_format,
            setNumber,
            numberFormat.format(set.weightKg),
            set.reps,
            set.rir,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
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
