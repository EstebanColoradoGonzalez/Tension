package com.estebancoloradogonzalez.tension.ui.metrics

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.BodyWeightSummary
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.ui.components.MetricCard
import com.estebancoloradogonzalez.tension.ui.components.MetricCardPair
import com.estebancoloradogonzalez.tension.ui.components.MetricEntityRow
import com.estebancoloradogonzalez.tension.ui.components.MetricInsufficientBlock
import com.estebancoloradogonzalez.tension.ui.components.MetricListCard
import com.estebancoloradogonzalez.tension.ui.components.MetricNavigationEntry
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirement
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirementKind
import com.estebancoloradogonzalez.tension.ui.components.MetricSectionHeader
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import com.estebancoloradogonzalez.tension.ui.components.MetricUnit
import com.estebancoloradogonzalez.tension.ui.components.MetricValue
import com.estebancoloradogonzalez.tension.ui.components.MetricValueText
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    onNavigateToOneRm: () -> Unit,
    onNavigateToVolume: () -> Unit,
    onNavigateToTrend: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: MetricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val weightSavedMsg = stringResource(R.string.metrics_body_weight_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MetricsEvent.WeightSaved -> snackbarHostState.showSnackbar(weightSavedMsg)
                is MetricsEvent.SaveError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    onNavigateToOneRm = onNavigateToOneRm,
                    onNavigateToVolume = onNavigateToVolume,
                    onNavigateToTrend = onNavigateToTrend,
                    onNavigateToExerciseHistory = onNavigateToExerciseHistory,
                    onOpenWeightDialog = viewModel::openWeightDialog,
                    modifier = Modifier.padding(innerPadding),
                )
                state.weightDialog?.let { dialog ->
                    RegisterWeightDialog(
                        dialog = dialog,
                        onInputChanged = viewModel::onWeightInputChanged,
                        onConfirm = viewModel::saveWeight,
                        onDismiss = viewModel::dismissWeightDialog,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsContent(
    state: MetricsUiState.Content,
    onChangeProgressionPeriod: (Int) -> Unit,
    onChangeRirPeriod: (Int) -> Unit,
    onNavigateToOneRm: () -> Unit,
    onNavigateToVolume: () -> Unit,
    onNavigateToTrend: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    onOpenWeightDialog: () -> Unit,
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

        // Section 4 — Body weight, a complement of the training indicators (HU-48). It
        // shares the period of Progression; nothing in the decision engine reads it.
        state.bodyWeight?.let { bodyWeight ->
            item {
                MetricSectionHeader(
                    title = stringResource(R.string.metrics_section_body_weight),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (bodyWeight.isStale) {
                item {
                    StaleWeightNote(
                        days = bodyWeight.daysSinceLastRecord,
                        onRegisterWeight = onOpenWeightDialog,
                    )
                }
            }
            item { CurrentWeightCard(bodyWeight = bodyWeight) }
            item {
                WeightVariationCard(
                    bodyWeight = bodyWeight,
                    weeks = state.progressionWeeks,
                    onChangePeriod = onChangeProgressionPeriod,
                )
            }
            item { BodyWeightChartCard(bodyWeight = bodyWeight, weeks = state.progressionWeeks) }
            item {
                Button(
                    onClick = onOpenWeightDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.metrics_body_weight_register))
                }
            }
        }

        // Accesos a las pantallas de detalle. Los tres son hermanos y se componen con el
        // mismo elemento para que no puedan divergir. El del 1RM se compone **siempre**,
        // con o sin sesiones registradas: el estado vacío lo resuelve la pantalla de
        // destino (CA-42.01).
        item {
            MetricNavigationEntry(
                title = stringResource(R.string.metrics_entry_one_rm_title),
                description = stringResource(R.string.metrics_entry_one_rm_description),
                onClick = onNavigateToOneRm,
            )
        }

        item {
            MetricNavigationEntry(
                title = stringResource(R.string.metrics_entry_volume_title),
                description = stringResource(R.string.metrics_entry_volume_description),
                onClick = onNavigateToVolume,
            )
        }

        item {
            MetricNavigationEntry(
                title = stringResource(R.string.metrics_entry_trend_title),
                description = stringResource(R.string.metrics_entry_trend_description),
                onClick = onNavigateToTrend,
            )
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
    MetricListCard(
        label = stringResource(R.string.metrics_progression_title),
        description = stringResource(R.string.metrics_progression_description),
        period = stringResource(R.string.metrics_period_last_weeks, weeks),
        header = {
            PeriodSelector(
                options = weekPeriodOptions(),
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

/**
 * Informative note, not an alert: it never reaches the Alert Center nor notifies, and it
 * disappears as soon as a new weight is registered (CA-48.06).
 */
@Composable
private fun StaleWeightNote(
    days: Long,
    onRegisterWeight: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.metrics_body_weight_stale, days.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRegisterWeight) {
                Text(stringResource(R.string.metrics_body_weight_register))
            }
        }
    }
}

/**
 * Current weight with the date it was registered, and the BMI as a secondary datum. The BMI
 * carries no category, colour or judgement: in hypertrophy gaining muscle raises it, and
 * labelling it would contradict the purpose of the system (CA-48.03, Rule 5).
 */
@Composable
private fun CurrentWeightCard(bodyWeight: BodyWeightSummary) {
    MetricListCard(
        label = stringResource(R.string.metrics_body_weight_current_title),
        description = stringResource(R.string.metrics_body_weight_current_description),
        period = stringResource(
            R.string.metrics_body_weight_recorded_on,
            bodyWeight.recordedOn.format(FULL_DATE),
        ),
    ) {
        MetricValueText(
            value = MetricValue.Available(bodyWeight.currentWeightKg, MetricUnit.BODY_WEIGHT),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.metrics_body_weight_bmi_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricValueText(
                value = MetricValue.Available(bodyWeight.bmi, MetricUnit.BMI),
                valueStyle = MaterialTheme.typography.titleMedium,
                unitStyle = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.metrics_body_weight_bmi_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeightVariationCard(
    bodyWeight: BodyWeightSummary,
    weeks: Int,
    onChangePeriod: (Int) -> Unit,
) {
    MetricListCard(
        label = stringResource(R.string.metrics_body_weight_variation_title),
        description = stringResource(R.string.metrics_body_weight_variation_description),
        period = stringResource(R.string.metrics_period_last_weeks, weeks),
        header = {
            PeriodSelector(
                options = weekPeriodOptions(),
                selected = weeks,
                onSelect = onChangePeriod,
            )
        },
    ) {
        MetricValueText(value = MetricSufficiencyRules.weightVariation(bodyWeight.variationKg))
    }
}

@Composable
private fun BodyWeightChartCard(bodyWeight: BodyWeightSummary, weeks: Int) {
    MetricListCard(
        label = stringResource(R.string.metrics_body_weight_chart_title),
        description = stringResource(R.string.metrics_body_weight_chart_description),
        period = stringResource(R.string.metrics_period_last_weeks, weeks),
    ) {
        BodyWeightChartComposable(
            periodStart = bodyWeight.periodStart,
            periodEnd = bodyWeight.periodEnd,
            weightPoints = bodyWeight.weightPoints,
            weeklyTonnage = bodyWeight.weeklyTonnage,
        )
    }
}

/**
 * Quick weight entry (CA-48.04, CA-48.05): one field in kg, prefilled with the last weight,
 * with the same criterion and the same inline message as the weight of the profile.
 */
@Composable
private fun RegisterWeightDialog(
    dialog: WeightDialogState,
    onInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.metrics_body_weight_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = dialog.input,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(R.string.weight_label)) },
                    suffix = { Text(stringResource(R.string.register_set_unit_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = dialog.showError,
                    supportingText = if (dialog.showError) {
                        { Text(stringResource(R.string.error_weight_positive)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.metrics_body_weight_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = dialog.canSave) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun weekPeriodOptions(): List<Pair<Int, String>> = listOf(
    4 to stringResource(R.string.metrics_period_4_weeks),
    8 to stringResource(R.string.metrics_period_8_weeks),
    12 to stringResource(R.string.metrics_period_12_weeks),
)

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

private val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private const val PROGRESSION_ASCENDING_THRESHOLD = 60.0
private const val PROGRESSION_STABLE_THRESHOLD = 40.0
