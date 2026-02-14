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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        ExerciseMediaSection(
            moduleCode = exercise.moduleCode,
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
            DetailField(
                label = stringResource(R.string.exercise_field_module),
                value = exercise.moduleName,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(
                label = stringResource(R.string.exercise_field_equipment),
                value = exercise.equipmentTypeName,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(
                label = stringResource(R.string.exercise_field_muscle_zone),
                value = exercise.muscleZones,
            )

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

@Composable
private fun ExerciseMediaSection(
    moduleCode: String,
    mediaResource: String?,
    onChangeImage: () -> Unit,
) {
    val bitmap = rememberExerciseBitmap(moduleCode = moduleCode, mediaResource = mediaResource)

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
                contentScale = ContentScale.Crop,
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
private fun rememberExerciseBitmap(moduleCode: String, mediaResource: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(moduleCode, mediaResource) {
        if (mediaResource == null) return@remember null
        // First try as absolute file path (custom exercise images)
        val file = File(mediaResource)
        if (file.exists()) {
            try {
                return@remember BitmapFactory.decodeFile(mediaResource)?.asImageBitmap()
            } catch (_: Exception) { /* fall through */ }
        }
        // Then try as asset path (seed exercise images)
        try {
            val path = "exercises/module-${moduleCode.lowercase()}/$mediaResource.png"
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
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
