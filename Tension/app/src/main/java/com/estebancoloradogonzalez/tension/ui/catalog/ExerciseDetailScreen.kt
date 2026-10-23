package com.estebancoloradogonzalez.tension.ui.catalog

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.ui.catalog.components.EquipmentMultiSelector
import com.estebancoloradogonzalez.tension.ui.catalog.components.MovementCategorySelector
import com.estebancoloradogonzalez.tension.ui.catalog.components.MuscleZoneHierarchySelector
import com.estebancoloradogonzalez.tension.ui.catalog.components.MuscleZoneHierarchySummary
import com.estebancoloradogonzalez.tension.ui.catalog.components.ProgressionDifficultySelector
import com.estebancoloradogonzalez.tension.ui.components.ExerciseImagePlaceholder
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar
import java.io.File

@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categoryConflict by viewModel.categoryConflict.collectAsStateWithLifecycle()
    val bodyWeightPercentForm by viewModel.bodyWeightPercentForm.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val percentSavedMessage = stringResource(R.string.exercise_body_weight_percent_saved)
    val percentSaveFailedMessage = stringResource(R.string.exercise_body_weight_percent_save_error)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ExerciseDetailEvent.BodyWeightPercentSaved ->
                    snackbarHostState.showSnackbar(percentSavedMessage)
                ExerciseDetailEvent.BodyWeightPercentSaveFailed ->
                    snackbarHostState.showSnackbar(percentSaveFailedMessage)
            }
        }
    }

    categoryConflict?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onCategoryConflictDismissed,
            title = { Text(stringResource(R.string.exercise_category_conflict_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::onCategoryConflictDismissed) {
                    Text(stringResource(R.string.category_conflict_dismiss))
                }
            },
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    Scaffold(
        topBar = {
            TensionTopAppBar(
                title = when (val state = uiState) {
                    is ExerciseDetailUiState.Success -> state.exercise.name
                    else -> ""
                },
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is ExerciseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ExerciseDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is ExerciseDetailUiState.Success -> {
                ExerciseDetailContent(
                    exercise = state.exercise,
                    onNavigateToHistory = onNavigateToExerciseHistory,
                    onChangeImage = { imagePickerLauncher.launch("image/*") },
                    onProgressionDifficultySelected = viewModel::onProgressionDifficultySelected,
                    bodyWeightPercentForm = bodyWeightPercentForm,
                    onBodyWeightPercentChanged = viewModel::onBodyWeightPercentChanged,
                    onSaveBodyWeightPercent = viewModel::saveBodyWeightPercent,
                    onMovementCategorySelected = viewModel::onMovementCategorySelected,
                    onEquipmentToggled = viewModel::onEquipmentToggled,
                    onPrimaryZoneAdded = viewModel::onPrimaryZoneAdded,
                    onPrimaryZoneRemoved = viewModel::onPrimaryZoneRemoved,
                    onSecondaryZoneAdded = viewModel::onSecondaryZoneAdded,
                    onSecondaryZoneRemoved = viewModel::onSecondaryZoneRemoved,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: ExerciseDetailItem,
    onNavigateToHistory: (Long) -> Unit,
    onChangeImage: () -> Unit,
    onProgressionDifficultySelected: (ProgressionDifficulty) -> Unit,
    bodyWeightPercentForm: BodyWeightPercentForm?,
    onBodyWeightPercentChanged: (String) -> Unit,
    onSaveBodyWeightPercent: () -> Unit,
    onMovementCategorySelected: (MovementCategory) -> Unit,
    onEquipmentToggled: (Long) -> Unit,
    onPrimaryZoneAdded: (Long) -> Unit,
    onPrimaryZoneRemoved: (Long) -> Unit,
    onSecondaryZoneAdded: (Long) -> Unit,
    onSecondaryZoneRemoved: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        ExerciseMediaSection(
            mediaResource = exercise.mediaResource,
            onChangeImage = onChangeImage,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            DetailField(
                label = stringResource(R.string.exercise_field_name),
                value = exercise.name,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Una sola categoría por ejercicio (CA-46.08). Editable en el sitio como la
            // dificultad; el conflicto con el plan se explica en un diálogo (CA-46.14).
            Text(
                text = stringResource(R.string.movement_category_field),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MovementCategorySelector(
                selected = exercise.movementCategory,
                onSelected = onMovementCategorySelected,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(
                label = stringResource(R.string.exercise_field_equipment),
                value = exercise.equipmentSummary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Editable en el sitio y sin botón de guardar, igual que la imagen y la
            // dificultad de progresión: la ficha persiste al instante y el flujo de Room
            // repinta. Un intento rechazado no escribe nada, así que la casilla vuelve.
            EquipmentMultiSelector(
                options = exercise.equipmentOptions,
                selectedIds = exercise.selectedEquipmentIds,
                onToggle = onEquipmentToggled,
                equipmentWithSets = exercise.equipmentWithSets,
                error = exercise.equipmentError,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Principales y secundarias como bloques distintos: la jerarquía se ve, que es
            // para lo que existe (CA-41.02).
            MuscleZoneHierarchySummary(
                primaryZones = exercise.primaryMuscleZones,
                secondaryZones = exercise.secondaryMuscleZones,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Editable en el sitio, igual que el equipamiento: persiste al instante y el
            // flujo de Room repinta. Un rechazo no escribe, así que el campo vuelve solo.
            MuscleZoneHierarchySelector(
                allZones = exercise.muscleZoneOptions,
                primaryZoneIds = exercise.selectedPrimaryZoneIds,
                secondaryZoneIds = exercise.selectedSecondaryZoneIds,
                onPrimaryZoneAdded = onPrimaryZoneAdded,
                onPrimaryZoneRemoved = onPrimaryZoneRemoved,
                onSecondaryZoneAdded = onSecondaryZoneAdded,
                onSecondaryZoneRemoved = onSecondaryZoneRemoved,
                error = exercise.muscleZoneError,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.exercise_field_progression_difficulty),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProgressionDifficultySelector(
                selectedDifficulty = exercise.progressionDifficulty,
                onDifficultySelected = onProgressionDifficultySelected,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.exercise_effective_plateau_threshold,
                    exercise.effectiveThresholdSessions,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.exercise_progression_difficulty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // An isometric is held in seconds and never adds body weight, so the field would
            // decide nothing there (HU-49, CA-49.07).
            if (!exercise.isIsometric && bodyWeightPercentForm != null) {
                Spacer(modifier = Modifier.height(16.dp))
                BodyWeightPercentField(
                    form = bodyWeightPercentForm,
                    onValueChange = onBodyWeightPercentChanged,
                    onSave = onSaveBodyWeightPercent,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { onNavigateToHistory(exercise.id) },
            ) {
                Text(
                    text = stringResource(R.string.exercise_history_link),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * «Peso corporal movido» (HU-49, CA-49.04): an integer percentage with an explicit save, unlike
 * the difficulty — saving recalculates the exercise's whole history (CA-49.08), so it should not
 * happen on every keystroke. An invalid value shows the inline error and disables saving
 * (CA-49.05).
 */
@Composable
private fun BodyWeightPercentField(
    form: BodyWeightPercentForm,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.exercise_field_body_weight_percent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = form.input,
            onValueChange = onValueChange,
            suffix = { Text(stringResource(R.string.exercise_body_weight_percent_suffix)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !form.isValid,
            supportingText = if (!form.isValid) {
                { Text(stringResource(R.string.exercise_body_weight_percent_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.exercise_body_weight_percent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSave,
            enabled = form.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.exercise_body_weight_percent_save))
        }
    }
}

@Composable
private fun ExerciseMediaSection(
    mediaResource: String?,
    onChangeImage: () -> Unit,
) {
    val bitmap = rememberExerciseBitmap(mediaResource = mediaResource)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .clickable { onChangeImage() },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.exercise_media_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            // Small overlay icon to indicate changeability
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = stringResource(R.string.change_image_description),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            ExerciseImagePlaceholder()
        }
    }
}

@Composable
private fun rememberExerciseBitmap(mediaResource: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(mediaResource) {
        if (mediaResource == null) return@remember null
        // First try as absolute file path (custom exercise images)
        val file = File(mediaResource)
        if (file.exists()) {
            try {
                return@remember BitmapFactory.decodeFile(mediaResource)?.asImageBitmap()
            } catch (_: Exception) { /* fall through */ }
        }
        // Then try as asset path (seed exercise images) - flat folder
        try {
            val path = "exercises/$mediaResource.png"
            return@remember context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) { /* no asset found */ }
        null
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
