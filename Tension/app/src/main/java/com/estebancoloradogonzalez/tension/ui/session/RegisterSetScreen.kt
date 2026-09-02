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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedIconButton
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
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.ui.components.CounterText
import com.estebancoloradogonzalez.tension.ui.components.EntityNameText
import com.estebancoloradogonzalez.tension.ui.session.components.IsometricChronometer
import com.estebancoloradogonzalez.tension.ui.session.components.WeightUnitSelector
import java.util.Locale

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
                expandedHeight = 96.dp,
                title = {
                    // The set counter is measured first (no weight), so it always keeps
                    // the space it needs; the exercise name is the one that yields.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EntityNameText(
                            text = uiState.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CounterText(
                            text = if (uiState.currentSetNumber > uiState.totalSets) {
                                stringResource(
                                    R.string.register_set_extra_title_format,
                                    uiState.currentSetNumber - uiState.totalSets,
                                )
                            } else {
                                stringResource(
                                    R.string.register_set_title_format,
                                    uiState.currentSetNumber,
                                    uiState.totalSets,
                                )
                            },
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

            WeightSection(
                uiState = uiState,
                onValueChange = viewModel::onWeightChanged,
                onUnitSelected = viewModel::onUnitSelected,
                onWeightStep = viewModel::onWeightStep,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.showChronometer) {
                IsometricChronometer(
                    timerState = uiState.timerState,
                    timerSeconds = uiState.timerSeconds,
                    minSeconds = uiState.minSeconds,
                    maxSeconds = uiState.maxSeconds,
                    onStartTimer = viewModel::onStartTimer,
                    onStopTimer = viewModel::onStopTimer,
                    onResetTimer = viewModel::onResetTimer,
                )
            } else {
                RepsField(uiState = uiState, onValueChange = viewModel::onRepsChanged)
            }

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

private fun unitLabelRes(unit: WeightUnit): Int = when (unit) {
    WeightUnit.KG -> R.string.register_set_unit_kg
    WeightUnit.LB -> R.string.register_set_unit_lb
}

@Composable
private fun WeightSection(
    uiState: RegisterSetUiState,
    onValueChange: (String) -> Unit,
    onUnitSelected: (WeightUnit) -> Unit,
    onWeightStep: (Boolean) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            WeightField(
                uiState = uiState,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
            )

            if (uiState.isUnitSelectorVisible) {
                Spacer(modifier = Modifier.width(8.dp))
                WeightUnitSelector(
                    selectedUnit = uiState.captureUnit,
                    onUnitSelected = onUnitSelected,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (uiState.isWeightEditable) {
            Spacer(modifier = Modifier.height(8.dp))
            WeightStepControls(unit = uiState.captureUnit, onWeightStep = onWeightStep)
        }
    }
}

@Composable
private fun WeightStepControls(
    unit: WeightUnit,
    onWeightStep: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(
            onClick = { onWeightStep(false) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.register_set_weight_decrease),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = { onWeightStep(true) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.register_set_weight_increase),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(
                R.string.register_set_increment_hint_format,
                stringResource(
                    if (unit == WeightUnit.LB) {
                        R.string.register_set_increment_lb
                    } else {
                        R.string.register_set_increment_kg
                    },
                ),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeightField(
    uiState: RegisterSetUiState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        uiState.isIsometric -> stringResource(R.string.register_set_weight_isometric_label)
        uiState.isBodyweight -> stringResource(R.string.register_set_weight_bodyweight_label)
        else -> stringResource(
            R.string.register_set_weight_label_format,
            stringResource(unitLabelRes(uiState.captureUnit)),
        )
    }

    // With the selector on screen the unit is already visible; the in-field suffix is
    // only needed for exercises without external load, where there is no selector.
    val trailingIcon: @Composable (() -> Unit)? = if (uiState.isUnitSelectorVisible) {
        null
    } else {
        {
            Text(
                text = stringResource(R.string.register_set_weight_suffix),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val convertedHint = uiState.convertedWeightKg
        ?.takeIf { uiState.captureUnit == WeightUnit.LB }

    val supportingText: @Composable (() -> Unit)? = when {
        uiState.weightError != null -> {
            { Text(uiState.weightError, color = MaterialTheme.colorScheme.error) }
        }
        convertedHint != null -> {
            {
                Text(
                    text = stringResource(
                        R.string.register_set_converted_hint_format,
                        String.format(Locale("es"), "%.2f", convertedHint),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> null
    }

    OutlinedTextField(
        value = uiState.weightInput,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = trailingIcon,
        isError = uiState.weightError != null,
        supportingText = supportingText,
        enabled = uiState.isWeightEditable,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
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
            for (rir in 0..2) {
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
