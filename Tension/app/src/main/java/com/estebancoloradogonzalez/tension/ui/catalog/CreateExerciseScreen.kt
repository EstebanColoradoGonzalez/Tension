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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.estebancoloradogonzalez.tension.ui.components.ExerciseImagePlaceholder
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                                    contentScale = ContentScale.Crop,
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

                    // Module dropdown
                    var moduleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = moduleExpanded,
                        onExpandedChange = { moduleExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedModuleName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.exercise_field_module)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleExpanded)
                            },
                            isError = uiState.moduleError != null,
                            supportingText = uiState.moduleError?.let { error ->
                                { Text(error) }
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = moduleExpanded,
                            onDismissRequest = { moduleExpanded = false },
                        ) {
                            uiState.modules.forEach { module ->
                                DropdownMenuItem(
                                    text = { Text(module.name) },
                                    onClick = {
                                        viewModel.onModuleSelected(module.code)
                                        moduleExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Equipment type dropdown
                    var equipmentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = equipmentExpanded,
                        onExpandedChange = { equipmentExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedEquipmentName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.exercise_field_equipment)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded)
                            },
                            isError = uiState.equipmentError != null,
                            supportingText = uiState.equipmentError?.let { error ->
                                { Text(error) }
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = equipmentExpanded,
                            onDismissRequest = { equipmentExpanded = false },
                        ) {
                            uiState.equipmentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        viewModel.onEquipmentTypeSelected(type.id)
                                        equipmentExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Muscle zones — multi-select chips
                    Text(
                        text = stringResource(R.string.exercise_field_muscle_zone),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (uiState.muscleZoneError != null) {
                        Text(
                            text = uiState.muscleZoneError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.muscleZones.forEach { zone ->
                            val selected = zone.id in uiState.selectedMuscleZoneIds
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onMuscleZoneToggled(zone.id) },
                                label = { Text(zone.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

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
                            Text(stringResource(R.string.create_exercise_button))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
