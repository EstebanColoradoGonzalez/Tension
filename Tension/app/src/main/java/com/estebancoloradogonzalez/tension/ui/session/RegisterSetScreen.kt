package com.estebancoloradogonzalez.tension.ui.session

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSetScreen(
    onNavigateBack: () -> Unit,
    viewModel: RegisterSetViewModel = hiltViewModel(),
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
                            text = uiState.exerciseName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(
                                R.string.register_set_title_format,
                                uiState.currentSetNumber,
                                uiState.totalSets,
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
                            contentDescription = stringResource(R.string.register_set_cancel),
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            WeightField(uiState = uiState, onValueChange = viewModel::onWeightChanged)

            Spacer(modifier = Modifier.height(16.dp))

            RepsField(uiState = uiState, onValueChange = viewModel::onRepsChanged)

            Spacer(modifier = Modifier.height(16.dp))

            RirSelector(
                selectedRir = uiState.selectedRir,
                onRirSelected = viewModel::onRirSelected,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onConfirm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = uiState.isConfirmEnabled,
            ) {
                Text(text = stringResource(R.string.register_set_confirm))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.register_set_cancel),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeightField(
    uiState: RegisterSetUiState,
    onValueChange: (String) -> Unit,
) {
    val label = when {
        uiState.isBodyweight -> stringResource(R.string.register_set_weight_bodyweight_label)
        uiState.isIsometric -> stringResource(R.string.register_set_weight_isometric_label)
        else -> stringResource(R.string.register_set_weight_label)
    }

    OutlinedTextField(
        value = uiState.weightKg,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = {
            Text(
                text = stringResource(R.string.register_set_weight_suffix),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        isError = uiState.weightError != null,
        supportingText = if (uiState.weightError != null) {
            { Text(uiState.weightError, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        enabled = uiState.isWeightEditable,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        colors = if (!uiState.isWeightEditable) {
            OutlinedTextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    .copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        },
    )
}

@Composable
private fun RepsField(
    uiState: RegisterSetUiState,
    onValueChange: (String) -> Unit,
) {
    val label = if (uiState.isIsometric) {
        stringResource(R.string.register_set_seconds_label)
    } else {
        stringResource(R.string.register_set_reps_label)
    }

    val suffix = if (uiState.isIsometric) {
        stringResource(R.string.register_set_seconds_suffix)
    } else {
        stringResource(R.string.register_set_reps_suffix)
    }

    val supportingText: @Composable (() -> Unit)? = when {
        uiState.repsError != null -> {
            { Text(uiState.repsError, color = MaterialTheme.colorScheme.error) }
        }
        uiState.isIsometric -> {
            {
                Text(
                    text = stringResource(R.string.register_set_seconds_reference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> null
    }

    OutlinedTextField(
        value = uiState.reps,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = {
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        isError = uiState.repsError != null,
        supportingText = supportingText,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RirSelector(
    selectedRir: Int?,
    onRirSelected: (Int) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.register_set_rir_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rir in 0..5) {
                val isSelected = rir == selectedRir
                val rirDescription = "RIR $rir"

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onRirSelected(rir) }
                        .semantics { contentDescription = rirDescription },
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = if (isSelected) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rir",
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
