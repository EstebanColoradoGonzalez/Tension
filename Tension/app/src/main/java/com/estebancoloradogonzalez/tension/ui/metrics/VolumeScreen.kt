package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTonnage
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot
import com.estebancoloradogonzalez.tension.ui.components.MetricInsufficientBlock
import com.estebancoloradogonzalez.tension.ui.components.MetricListCard
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirement
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirementKind
import com.estebancoloradogonzalez.tension.ui.components.MetricSectionHeader
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import com.estebancoloradogonzalez.tension.ui.components.MetricValue
import com.estebancoloradogonzalez.tension.ui.components.MetricValueText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeScreen(
    onNavigateBack: () -> Unit,
    viewModel: VolumeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.volume_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is VolumeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is VolumeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is VolumeUiState.Content -> {
                VolumeContent(
                    state = state,
                    onSelectMicrocycle = viewModel::selectMicrocycle,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun VolumeContent(
    state: VolumeUiState.Content,
    onSelectMicrocycle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val microcyclePeriod = stringResource(R.string.volume_period_microcycle, state.selectedMicrocycle)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            MicrocycleSelector(
                selected = state.selectedMicrocycle,
                total = state.totalMicrocycles,
                onSelect = onSelectMicrocycle,
            )
        }

        // Section 1 — Tonnage by muscle group
        item { MetricSectionHeader(title = stringResource(R.string.volume_section_tonnage)) }
        item {
            TonnageByGroupCard(
                tonnageByGroup = state.tonnageByGroup,
                sessionsInMicrocycle = state.sessionsInSelectedMicrocycle,
                period = microcyclePeriod,
            )
        }

        // Section 2 — Volume distribution by muscle zone
        item {
            MetricSectionHeader(
                title = stringResource(R.string.volume_section_distribution),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            VolumeDistributionCard(
                distributionByMuscleGroup = state.distributionByMuscleGroup,
                sessionsInMicrocycle = state.sessionsInSelectedMicrocycle,
                period = microcyclePeriod,
            )
        }

        // Section 3 — Tonnage evolution
        item {
            MetricSectionHeader(
                title = stringResource(R.string.volume_section_evolution),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            EvolutionCard(
                evolution = state.evolution,
                totalMicrocycles = state.totalMicrocycles,
                insufficientEvolution = state.insufficientEvolution,
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun MicrocycleSelector(
    selected: Int,
    total: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onSelect(selected - 1) },
            enabled = selected > 1,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = if (selected > 1) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
        Text(
            text = stringResource(R.string.volume_microcycle_label, selected),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { onSelect(selected + 1) },
            enabled = selected < total,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (selected < total) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Composable
private fun TonnageByGroupCard(
    tonnageByGroup: List<MuscleGroupTonnage>,
    sessionsInMicrocycle: Int,
    period: String,
) {
    val maxTonnage = tonnageByGroup.maxOfOrNull { it.tonnageKg }?.coerceAtLeast(1.0) ?: 1.0

    MetricListCard(
        label = stringResource(R.string.volume_tonnage_title),
        description = stringResource(R.string.volume_tonnage_description),
        period = period,
    ) {
        if (sessionsInMicrocycle < MetricSufficiencyRules.MIN_MICROCYCLE_SESSIONS) {
            MetricInsufficientBlock(
                requirement = MetricRequirement(
                    kind = MetricRequirementKind.MICROCYCLE_SESSIONS,
                    available = sessionsInMicrocycle,
                    needed = MetricSufficiencyRules.MIN_MICROCYCLE_SESSIONS,
                ),
            )
        } else {
            tonnageByGroup.forEach { item ->
                MetricBarRow(
                    label = item.muscleGroup,
                    fraction = (item.tonnageKg / maxTonnage).toFloat(),
                    value = MetricSufficiencyRules.tonnage(item.tonnageKg, sessionsInMicrocycle),
                    barColor = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VolumeDistributionCard(
    distributionByMuscleGroup: Map<String, Map<String, Double>>,
    sessionsInMicrocycle: Int,
    period: String,
) {
    MetricListCard(
        label = stringResource(R.string.volume_distribution_title),
        description = stringResource(R.string.volume_distribution_description),
        period = period,
    ) {
        if (sessionsInMicrocycle < MetricSufficiencyRules.MIN_MICROCYCLE_SESSIONS ||
            distributionByMuscleGroup.isEmpty()
        ) {
            MetricInsufficientBlock(
                requirement = MetricRequirement(
                    kind = MetricRequirementKind.MICROCYCLE_SESSIONS,
                    available = sessionsInMicrocycle,
                    needed = MetricSufficiencyRules.MIN_MICROCYCLE_SESSIONS,
                ),
            )
        } else {
            distributionByMuscleGroup.toSortedMap().forEach { (muscleGroup, zones) ->
                Text(
                    text = stringResource(R.string.volume_routine_header, muscleGroup),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))

                zones.toSortedMap().forEach { (zoneName, percentage) ->
                    MetricBarRow(
                        label = zoneName,
                        fraction = (percentage / 100.0).toFloat(),
                        value = MetricSufficiencyRules.distribution(percentage, sessionsInMicrocycle),
                        barColor = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MetricBarRow(
    label: String,
    fraction: Float,
    value: MetricValue,
    barColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(110.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        MetricValueText(
            value = value,
            valueStyle = MaterialTheme.typography.bodyMedium,
            unitStyle = MaterialTheme.typography.labelSmall,
            showRequirement = false,
        )
    }
}

@Composable
private fun EvolutionCard(
    evolution: List<TonnageSnapshot>,
    totalMicrocycles: Int,
    insufficientEvolution: Boolean,
) {
    MetricListCard(
        label = stringResource(R.string.volume_evolution_title),
        description = stringResource(R.string.volume_evolution_description),
        period = stringResource(R.string.volume_period_all_microcycles),
    ) {
        val requirement = MetricSufficiencyRules.evolution(totalMicrocycles)
        if (insufficientEvolution && requirement != null) {
            MetricInsufficientBlock(requirement = requirement)
        } else {
            TonnageChartComposable(
                snapshots = evolution,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
