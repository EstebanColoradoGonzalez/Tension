package com.estebancoloradogonzalez.tension.ui.catalog

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanVersionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit,
    viewModel: PlanVersionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()
    val exerciseToDelete by viewModel.deleteDialogState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val addAlternativeState by viewModel.addAlternativeState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onUserMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.routineName.isNotBlank()) {
                            stringResource(
                                R.string.plan_version_title_format,
                                uiState.routineName,
                                uiState.versionNumber,
                            )
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onFabClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.fab_assign_exercise),
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Text(
                        text = stringResource(
                            R.string.plan_exercise_count_subtitle,
                            uiState.exercises.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp,
                        ),
                    )

                    if (uiState.exercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_exercises_assigned),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        PlanExerciseList(
                            exercises = uiState.exercises,
                            onExerciseClick = onNavigateToExerciseDetail,
                            onDeleteClick = viewModel::onDeleteExercise,
                            onEditClick = viewModel::onEditExercise,
                            onAddAlternativeClick = viewModel::onAddAlternativeClick,
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    exerciseToDelete?.let { exercise ->
        val displayName = if (exercise.alternativeNames.isEmpty()) {
            exercise.name
        } else {
            (listOf(exercise.name) + exercise.alternativeNames).joinToString(" ó ")
        }
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteDialog,
            title = { Text(stringResource(R.string.unassign_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.unassign_dialog_message,
                        displayName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.unassign_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Assign exercise bottom sheet
    if (sheetState.isVisible) {
        AssignExerciseSheet(
            sheetState = sheetState,
            onDismiss = viewModel::onDismissSheet,
            onExerciseSelected = viewModel::onExerciseSelected,
            onSetsChanged = viewModel::onSetsChanged,
            onRepsSelected = viewModel::onRepsSelected,
            onConfirmAssign = viewModel::onConfirmAssign,
        )
    }

    // Edit plan assignment dialog
    if (editState.isVisible) {
        EditPlanAssignmentDialog(
            state = editState,
            onSetsChanged = viewModel::onEditSetsChanged,
            onRepsSelected = viewModel::onEditRepsSelected,
            onConfirm = viewModel::onConfirmEdit,
            onDismiss = viewModel::onDismissEdit,
        )
    }

    // Add alternative to slot bottom sheet
    if (addAlternativeState.isVisible) {
        AddAlternativeSheet(
            state = addAlternativeState,
            onDismiss = viewModel::onDismissAddAlternative,
            onExerciseSelected = viewModel::onAlternativeExerciseSelected,
            onConfirm = viewModel::onConfirmAddAlternative,
        )
    }
}

@Composable
private fun PlanExerciseList(
    exercises: List<PlanExerciseItem>,
    onExerciseClick: (Long) -> Unit,
    onDeleteClick: (PlanExerciseItem) -> Unit,
    onEditClick: (PlanExerciseItem) -> Unit,
    onAddAlternativeClick: (PlanExerciseItem) -> Unit,
) {
    LazyColumn {
        itemsIndexed(exercises) { index, exercise ->
            val headline = if (exercise.alternativeNames.isEmpty()) {
                exercise.name
            } else {
                (listOf(exercise.name) + exercise.alternativeNames).joinToString(" ó ")
            }
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (exercise.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_custom),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (exercise.isBodyweight) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.exercise_outside_gym),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "${exercise.muscleZonesSummary} · ${exercise.equipmentTypeName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                R.string.prescription_format,
                                exercise.sets,
                                exercise.repsDisplay,
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = if (exercise.isSpecialCondition) FontStyle.Italic else FontStyle.Normal,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { onAddAlternativeClick(exercise) }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_alternative_description),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = { onEditClick(exercise) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onDeleteClick(exercise) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.unassign_dialog_title),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { onExerciseClick(exercise.exerciseId) },
            )
            if (index < exercises.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignExerciseSheet(
    sheetState: AssignExerciseSheetState,
    onDismiss: () -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onSetsChanged: (String) -> Unit,
    onRepsSelected: (String) -> Unit,
    onConfirmAssign: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.assign_exercise_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (sheetState.availableExercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.assign_exercise_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                if (sheetState.selectedExerciseId == null) {
                    // Step 1: Select exercise
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                    ) {
                        itemsIndexed(sheetState.availableExercises) { index, exercise ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${exercise.muscleZonesSummary} · ${exercise.equipmentTypeName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onExerciseSelected(exercise.id)
                                },
                            )
                            if (index < sheetState.availableExercises.lastIndex) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                } else {
                    // Step 2: Configure sets & reps
                    val selectedExercise = sheetState.availableExercises.find {
                        it.id == sheetState.selectedExerciseId
                    }

                    selectedExercise?.let { exercise ->
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    OutlinedTextField(
                        value = sheetState.sets,
                        onValueChange = onSetsChanged,
                        label = { Text(stringResource(R.string.sets_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.reps_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RepsOption(
                            text = "8-12 reps",
                            selected = sheetState.reps == "8-12",
                            onClick = { onRepsSelected("8-12") },
                            modifier = Modifier.weight(1f),
                        )
                        RepsOption(
                            text = "Al fallo",
                            selected = sheetState.reps == "TO_TECHNICAL_FAILURE",
                            onClick = { onRepsSelected("TO_TECHNICAL_FAILURE") },
                            modifier = Modifier.weight(1f),
                        )
                        RepsOption(
                            text = "30\u201345 seg",
                            selected = sheetState.reps == "30-45_SEC",
                            onClick = { onRepsSelected("30-45_SEC") },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FilledTonalButton(
                        onClick = onConfirmAssign,
                        enabled = !sheetState.isAssigning &&
                            (sheetState.sets.toIntOrNull() ?: 0) > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.assign_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPlanAssignmentDialog(
    state: EditPlanAssignmentState,
    onSetsChanged: (Int) -> Unit,
    onRepsSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_assignment_title)) },
        text = {
            Column {
                Text(
                    text = state.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = stringResource(R.string.sets_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (s in 1..10) {
                        val isSelected = s == state.sets
                        Surface(
                            onClick = { onSetsChanged(s) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.reps_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RepsOption(
                        text = "8-12 reps",
                        selected = state.reps == "8-12",
                        onClick = { onRepsSelected("8-12") },
                        modifier = Modifier.weight(1f),
                    )
                    RepsOption(
                        text = "Al fallo",
                        selected = state.reps == "TO_TECHNICAL_FAILURE",
                        onClick = { onRepsSelected("TO_TECHNICAL_FAILURE") },
                        modifier = Modifier.weight(1f),
                    )
                    RepsOption(
                        text = "30\u201345 seg",
                        selected = state.reps == "30-45_SEC",
                        onClick = { onRepsSelected("30-45_SEC") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !state.isSaving,
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RepsOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier.height(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlternativeSheet(
    state: AddAlternativeSheetState,
    onDismiss: () -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.add_alternative_title, state.slotName),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.add_alternative_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (state.availableExercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.assign_exercise_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(state.availableExercises) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text("${exercise.muscleZonesSummary} · ${exercise.equipmentTypeName}")
                            },
                            modifier = Modifier.clickable { onExerciseSelected(exercise.id) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = if (state.selectedExerciseId == exercise.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                        )
                    }
                }
            }
            FilledTonalButton(
                onClick = onConfirm,
                enabled = state.selectedExerciseId != null && !state.isAssigning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(stringResource(R.string.add_alternative_button))
            }
        }
    }
}
