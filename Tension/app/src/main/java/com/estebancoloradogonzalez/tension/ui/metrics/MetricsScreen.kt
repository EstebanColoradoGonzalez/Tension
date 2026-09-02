package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.ui.components.MetricCard
import com.estebancoloradogonzalez.tension.ui.components.MetricCardPair
import com.estebancoloradogonzalez.tension.ui.components.MetricEntityRow
import com.estebancoloradogonzalez.tension.ui.components.MetricInsufficientBlock
import com.estebancoloradogonzalez.tension.ui.components.MetricListCard
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirement
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirementKind
import com.estebancoloradogonzalez.tension.ui.components.MetricSectionHeader
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    onNavigateToVolume: () -> Unit,
    onNavigateToTrend: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: MetricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.metrics_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is MetricsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is MetricsUiState.Error -> {
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
            is MetricsUiState.Content -> {
                MetricsContent(
                    state = state,
                    onChangeProgressionPeriod = viewModel::changeProgressionPeriod,
                    onChangeRirPeriod = viewModel::changeRirPeriod,
                    onNavigateToVolume = onNavigateToVolume,
                    onNavigateToTrend = onNavigateToTrend,
                    onNavigateToExerciseHistory = onNavigateToExerciseHistory,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun MetricsContent(
    state: MetricsUiState.Content,
    onChangeProgressionPeriod: (Int) -> Unit,
    onChangeRirPeriod: (Int) -> Unit,
    onNavigateToVolume: () -> Unit,
    onNavigateToTrend: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Section 1 — Adherence
        item { MetricSectionHeader(title = stringResource(R.string.metrics_section_adherence)) }
        item { AdherenceSection(adherence = state.adherence) }

        // Section 2 — Intensity
        item {
            MetricSectionHeader(
                title = stringResource(R.string.metrics_section_intensity),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            RirCard(
                rirByRoutine = state.rirByRoutine,
                sessionLimit = state.rirSessionLimit,
                onChangeRirPeriod = onChangeRirPeriod,
            )
        }

        // Section 3 — Progression
        item {
            MetricSectionHeader(
                title = stringResource(R.string.metrics_section_progression),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            ProgressionRateCard(
                rates = state.progressionRates,
                weeks = state.progressionWeeks,
                onChangePeriod = onChangeProgressionPeriod,
                onExerciseClick = onNavigateToExerciseHistory,
            )
        }
        item {
            LoadVelocityCard(
                velocities = state.loadVelocities,
                weeks = state.progressionWeeks,
                onExerciseClick = onNavigateToExerciseHistory,
            )
        }

        // Quick links
        item {
            TextButton(
                onClick = onNavigateToVolume,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.metrics_link_volume),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            TextButton(
                onClick = onNavigateToTrend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.metrics_link_trend),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun AdherenceSection(adherence: AdherenceData) {
    MetricCardPair(
        left = { cardModifier ->
            MetricCard(
                label = stringResource(R.string.metrics_adherence_title),
                value = MetricSufficiencyRules.adherence(
                    percentage = adherence.percentage,
                    plannedSessions = adherence.plannedSessions,
                ),
                description = stringResource(
                    R.string.metrics_adherence_description,
                    adherence.completedSessions,
                    adherence.plannedSessions,
                ),
                period = stringResource(R.string.metrics_period_current_week),
                modifier = cardModifier,
            )
        },
    )
}

@Composable
private fun RirCard(
    rirByRoutine: List<RirByRoutine>,
    sessionLimit: Int,
    onChangeRirPeriod: (Int) -> Unit,
) {
    val options = listOf(
        2 to stringResource(R.string.metrics_rir_2_sessions),
        4 to stringResource(R.string.metrics_rir_4_sessions),
        6 to stringResource(R.string.metrics_rir_6_sessions),
    )

    MetricListCard(
        label = stringResource(R.string.metrics_rir_title),
        description = stringResource(R.string.metrics_rir_description),
        period = stringResource(R.string.metrics_period_last_sessions, sessionLimit),
        header = {
            PeriodSelector(
                options = options,
                selected = sessionLimit,
                onSelect = onChangeRirPeriod,
            )
        },
    ) {
        if (rirByRoutine.isEmpty()) {
            MetricInsufficientBlock(
                requirement = MetricRequirement(
                    kind = MetricRequirementKind.ROUTINE_SETS,
                    available = 0,
                    needed = MetricSufficiencyRules.MIN_RIR_SETS,
                ),
            )
        } else {
            rirByRoutine.forEach { routine ->
                MetricEntityRow(
                    name = routine.routineName,
                    value = MetricSufficiencyRules.averageRir(
                        averageRir = routine.averageRir,
                        recordedSets = routine.recordedSets,
                    ),
                    trailing = if (routine.interpretation != null) {
                        { RirInterpretationBadge(routine.interpretation) }
                    } else {
                        null
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.metrics_rir_reference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RirInterpretationBadge(interpretation: RirInterpretation) {
    val semanticColors = TensionThemeExtended.semanticColors
    val (badgeColor, badgeLabelColor, badgeText) = when (interpretation) {
        RirInterpretation.OPTIMAL -> Triple(
            semanticColors.exerciseRowCompletedBg,
            semanticColors.progressionPositive,
            stringResource(R.string.metrics_rir_optimal),
        )
        RirInterpretation.RISK_TOO_CLOSE -> Triple(
            semanticColors.alertCrisisBg,
            semanticColors.alertCrisis,
            stringResource(R.string.metrics_rir_risk),
        )
        RirInterpretation.INSUFFICIENT_STIMULUS -> Triple(
            semanticColors.alertMediumBg,
            semanticColors.alertMedium,
            stringResource(R.string.metrics_rir_insufficient),
        )
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = badgeColor,
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = badgeLabelColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ProgressionRateCard(
    rates: List<ExerciseProgressionRate>,
    weeks: Int,
    onChangePeriod: (Int) -> Unit,
    onExerciseClick: (Long) -> Unit,
) {
    val options = listOf(
        4 to stringResource(R.string.metrics_period_4_weeks),
        8 to stringResource(R.string.metrics_period_8_weeks),
        12 to stringResource(R.string.metrics_period_12_weeks),
    )

    MetricListCard(
        label = stringResource(R.string.metrics_progression_title),
        description = stringResource(R.string.metrics_progression_description),
        period = stringResource(R.string.metrics_period_last_weeks, weeks),
        header = {
            PeriodSelector(
                options = options,
                selected = weeks,
                onSelect = onChangePeriod,
            )
        },
    ) {
        if (rates.isEmpty()) {
            MetricInsufficientBlock(
                requirement = MetricRequirement(
                    kind = MetricRequirementKind.EXERCISE_OBSERVATIONS,
                    available = 0,
                    needed = MetricSufficiencyRules.MIN_PROGRESSION_OBSERVATIONS,
                ),
            )
        } else {
            rates.forEach { rate ->
                MetricEntityRow(
                    name = rate.exerciseName,
                    value = MetricSufficiencyRules.progressionRate(
                        rate = rate.rate,
                        observations = rate.observations,
                    ),
                    modifier = Modifier.clickable { onExerciseClick(rate.exerciseId) },
                    trailing = if (rate.observations >= MetricSufficiencyRules.MIN_PROGRESSION_OBSERVATIONS) {
                        { ProgressionTrendIcon(rate.rate) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ProgressionTrendIcon(rate: Double) {
    val semanticColors = TensionThemeExtended.semanticColors
    val (trendIcon, trendColor) = when {
        rate >= PROGRESSION_ASCENDING_THRESHOLD -> "↑" to semanticColors.progressionPositive
        rate >= PROGRESSION_STABLE_THRESHOLD -> "=" to semanticColors.maintenance
        else -> "↓" to semanticColors.regression
    }
    Text(
        text = trendIcon,
        color = trendColor,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun LoadVelocityCard(
    velocities: List<ExerciseLoadVelocity>,
    weeks: Int,
    onExerciseClick: (Long) -> Unit,
) {
    MetricListCard(
        label = stringResource(R.string.metrics_load_velocity_title),
        description = stringResource(R.string.metrics_load_velocity_description),
        period = stringResource(R.string.metrics_period_last_weeks, weeks),
    ) {
        if (velocities.isEmpty()) {
            MetricInsufficientBlock(
                requirement = MetricRequirement(
                    kind = MetricRequirementKind.EXERCISE_SESSIONS,
                    available = 0,
                    needed = MetricSufficiencyRules.MIN_LOAD_VELOCITY_SESSIONS,
                ),
            )
        } else {
            velocities.forEach { velocity ->
                MetricEntityRow(
                    name = velocity.exerciseName,
                    value = MetricSufficiencyRules.loadVelocity(
                        velocity = velocity.velocity,
                        sessionCount = velocity.sessionCount,
                        isBodyweight = velocity.isBodyweight,
                    ),
                    modifier = Modifier.clickable { onExerciseClick(velocity.exerciseId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        expanded = false
                        onSelect(option.first)
                    },
                )
            }
        }
    }
}

private const val PROGRESSION_ASCENDING_THRESHOLD = 60.0
private const val PROGRESSION_STABLE_THRESHOLD = 40.0
