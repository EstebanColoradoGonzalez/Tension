package com.estebancoloradogonzalez.tension.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstituteExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubstituteExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { success ->
            if (success) onNavigateBack()
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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.substitute_exercise_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(
                                R.string.substitute_exercise_subtitle_format,
                                uiState.originalExerciseName,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.substitute_exercise_cancel),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.substitute_exercise_info_line1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.substitute_exercise_info_line2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.eligibleExercises) { index, exercise ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${exercise.muscleZones} · ${exercise.equipmentTypeName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.onExerciseSelected(exercise)
                        },
                    )
                    if (index < uiState.eligibleExercises.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp, bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.substitute_exercise_cancel),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (uiState.showConfirmDialog && uiState.selectedExercise != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDialog() },
            title = {
                Text(
                    text = stringResource(
                        R.string.substitute_exercise_dialog_title_format,
                        uiState.originalExerciseName,
                        uiState.selectedExercise!!.name,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.substitute_exercise_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmSubstitution() },
                    enabled = !uiState.isSubstituting,
                ) {
                    Text(stringResource(R.string.substitute_exercise_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissDialog() }) {
                    Text(stringResource(R.string.substitute_exercise_cancel))
                }
            },
        )
    }
}
