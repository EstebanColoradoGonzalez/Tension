package com.estebancoloradogonzalez.tension.ui.onerm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ExerciseOneRm
import com.estebancoloradogonzalez.tension.domain.model.OneRmEquipment
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.util.WeightConverter

private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val CARD_PADDING = 16.dp

/**
 * Estimated 1RM per (exercise, equipment) pair.
 *
 * One card per exercise, in alphabetical order: the list is not multiplied by implement, and
 * the number stays visible without an intermediate navigation step. The only navigation
 * action is the native back arrow — here nothing is decided, only looked at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmScreen(
    onNavigateBack: () -> Unit,
    viewModel: OneRmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.one_rm_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
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
            uiState.isEmpty -> OneRmEmptyState(modifier = Modifier.padding(innerPadding))
            else -> OneRmContent(
                state = uiState,
                onSelectEquipment = viewModel::selectEquipment,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun OneRmContent(
    state: OneRmUiState,
    onSelectEquipment: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        items(items = state.exercises, key = { it.exerciseId }) { exercise ->
            OneRmCard(
                exercise = exercise,
                selected = state.selectedEquipment(exercise),
                onSelectEquipment = { equipmentTypeId ->
                    onSelectEquipment(exercise.exerciseId, equipmentTypeId)
                },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * One exercise: its name, the implements it holds a record for and the value of the active
 * one. Changing implement changes the number in place — there is nowhere to navigate to.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OneRmCard(
    exercise: ExerciseOneRm,
    selected: OneRmEquipment,
    onSelectEquipment: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (exercise.isSelectable) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.equipment.forEach { equipment ->
                        FilterChip(
                            selected = equipment.equipmentTypeId == selected.equipmentTypeId,
                            onClick = { onSelectEquipment(equipment.equipmentTypeId) },
                            label = { Text(text = equipment.equipmentTypeName) },
                        )
                    }
                }
            } else {
                // A single trained implement is not a choice, so it is not offered as one.
                Text(
                    text = selected.equipmentTypeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = oneRmValueText(selected.oneRmKg),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Kilograms and pounds at once, no toggle (CA-42.06).
 *
 * Pounds are derived here and never stored: the kilogram is the canonical unit and
 * [WeightConverter] already holds the fixed factor. The capture unit of the set that produced
 * the record plays no part — both units are always shown.
 */
@Composable
private fun oneRmValueText(oneRmKg: Double): String = stringResource(
    R.string.one_rm_value_format,
    "%.1f".format(oneRmKg),
    "%.1f".format(WeightConverter.fromKg(oneRmKg, WeightUnit.LB)),
)

/**
 * Nothing has ever qualified.
 *
 * It states the condition instead of leaving a blank list: HU-35 showed that a metric without
 * context is not understood, and an empty screen would leave the executant without knowing
 * what to do to fill it (CA-42.07).
 */
@Composable
private fun OneRmEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.one_rm_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.one_rm_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
