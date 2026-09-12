package com.estebancoloradogonzalez.tension.ui.catalog

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.catalog.components.EquipmentMultiSelector
import com.estebancoloradogonzalez.tension.ui.catalog.components.MuscleZoneHierarchySelector
import com.estebancoloradogonzalez.tension.ui.catalog.components.ProgressionDifficultySelector
import com.estebancoloradogonzalez.tension.ui.components.ExerciseImagePlaceholder
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onDismissSaveError()
        }
    }

    Scaffold(
        topBar = {
            TensionTopAppBar(
                title = stringResource(R.string.create_exercise_title),
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Image section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        val imagePath = uiState.imageUri
                        if (imagePath != null) {
                            val bitmap = remember(imagePath) {
                                try {
                                    BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.exercise_media_description),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                ExerciseImagePlaceholder()
                            }
                        } else {
                            ExerciseImagePlaceholder()
                        }
                    }
                    Text(
                        text = stringResource(R.string.image_optional_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Name field
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text(stringResource(R.string.exercise_field_name)) },
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { error ->
                            { Text(error) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Equipamiento admitido — selección múltiple, obligatoria
                    Text(
                        text = stringResource(R.string.exercise_field_equipment),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    EquipmentMultiSelector(
                        options = uiState.equipmentTypes,
                        selectedIds = uiState.selectedEquipmentTypeIds,
                        onToggle = viewModel::onEquipmentTypeToggled,
                        error = uiState.equipmentError,
                    )

                    // Zonas musculares con jerarquía: dos campos, el error donde se incumple
                    MuscleZoneHierarchySelector(
                        allZones = uiState.muscleZones,
                        primaryZoneIds = uiState.primaryMuscleZoneIds,
                        secondaryZoneIds = uiState.secondaryMuscleZoneIds,
                        onPrimaryZoneAdded = viewModel::onPrimaryMuscleZoneAdded,
                        onPrimaryZoneRemoved = viewModel::onPrimaryMuscleZoneRemoved,
                        onSecondaryZoneAdded = viewModel::onSecondaryMuscleZoneAdded,
                        onSecondaryZoneRemoved = viewModel::onSecondaryMuscleZoneRemoved,
                        error = uiState.muscleZoneError,
                    )

                    // Progression difficulty — MEDIUM preselected
                    Text(
                        text = stringResource(R.string.exercise_field_progression_difficulty),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    ProgressionDifficultySelector(
                        selectedDifficulty = uiState.progressionDifficulty,
                        onDifficultySelected = viewModel::onProgressionDifficultyChanged,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.create_exercise_difficulty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Special condition checkboxes
                    Text(
                        text = stringResource(R.string.special_conditions_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.isBodyweight,
                            onCheckedChange = viewModel::onBodyweightChanged,
                        )
                        Text(
                            text = stringResource(R.string.bodyweight_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                viewModel.onBodyweightChanged(!uiState.isBodyweight)
                            },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.isIsometric,
                            onCheckedChange = viewModel::onIsometricChanged,
                        )
                        Text(
                            text = stringResource(R.string.isometric_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                viewModel.onIsometricChanged(!uiState.isIsometric)
                            },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.isToTechnicalFailure,
                            onCheckedChange = viewModel::onToTechnicalFailureChanged,
                        )
                        Text(
                            text = stringResource(R.string.technical_failure_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                viewModel.onToTechnicalFailureChanged(!uiState.isToTechnicalFailure)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save button
                    Button(
                        onClick = viewModel::onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (uiState.isFromSession) {
                                        R.string.create_exercise_save_and_add
                                    } else {
                                        R.string.create_exercise_button
                                    },
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
