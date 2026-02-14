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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDictionaryScreen(
    onNavigateToExerciseDetail: (Long) -> Unit,
    onNavigateToTrainingPlan: () -> Unit,
    onNavigateToCreateExercise: () -> Unit,
    viewModel: ExerciseDictionaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dictionary_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                )
                TabRow(
                    selectedTabIndex = 0,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
                            width = tabPositions[0].contentWidth,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    Tab(
                        selected = true,
                        onClick = { },
                        text = {
                            Text(
                                text = stringResource(R.string.tab_exercises),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    Tab(
                        selected = false,
                        onClick = onNavigateToTrainingPlan,
                        text = {
                            Text(
                                text = stringResource(R.string.tab_plan),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateExercise,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.fab_create_exercise),
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
                    FilterSection(
                        uiState = uiState,
                        onModuleSelected = viewModel::onModuleFilterSelected,
                        onEquipmentSelected = viewModel::onEquipmentFilterSelected,
                        onMuscleZoneSelected = viewModel::onMuscleZoneFilterSelected,
                    )

                    Text(
                        text = stringResource(
                            R.string.exercise_count_format,
                            uiState.exercises.size,
                            uiState.totalCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                    )

                    if (uiState.exercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_exercises_match),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        ExerciseList(
                            exercises = uiState.exercises,
                            onExerciseClick = onNavigateToExerciseDetail,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    uiState: ExerciseDictionaryUiState,
    onModuleSelected: (String?) -> Unit,
    onEquipmentSelected: (String?) -> Unit,
    onMuscleZoneSelected: (String?) -> Unit,
) {
    val allLabel = stringResource(R.string.filter_all)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactDropdown(
            label = stringResource(R.string.filter_module),
            options = uiState.moduleOptions,
            selectedOption = uiState.selectedModule,
            allLabel = allLabel,
            onOptionSelected = onModuleSelected,
            modifier = Modifier.weight(1f),
        )
        CompactDropdown(
            label = stringResource(R.string.filter_equipment),
            options = uiState.equipmentOptions,
            selectedOption = uiState.selectedEquipment,
            allLabel = allLabel,
            onOptionSelected = onEquipmentSelected,
            modifier = Modifier.weight(1f),
        )
        CompactDropdown(
            label = stringResource(R.string.filter_muscle_zone),
            options = uiState.muscleZoneOptions,
            selectedOption = uiState.selectedMuscleZone,
            allLabel = allLabel,
            onOptionSelected = onMuscleZoneSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    allLabel: String,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedOption ?: allLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 13.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onOptionSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<ExerciseItem>,
    onExerciseClick: (Long) -> Unit,
) {
    LazyColumn {
        itemsIndexed(exercises) { index, exercise ->
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    }
                },
                supportingContent = {
                    Text(
                        text = stringResource(
                            R.string.exercise_supporting_format,
                            exercise.moduleCode,
                            exercise.equipmentTypeName,
                            exercise.muscleZonesSummary,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier
                    .clickable { onExerciseClick(exercise.id) }
                    .height(72.dp),
            )
            if (index < exercises.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
