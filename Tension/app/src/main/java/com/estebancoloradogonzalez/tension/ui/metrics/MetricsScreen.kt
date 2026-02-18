package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByModule
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation

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
        item {
            AdherenceCard(adherence = state.adherence)
        }

        // Section 2 — RIR by Module
        item {
            RirByModuleCard(
                rirByModule = state.rirByModule,
                onChangeRirPeriod = onChangeRirPeriod,
            )
        }

        // Divider 2↔3
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Section 3 — Progression Rate
        item {
            ProgressionRateSection(
                rates = state.progressionRates,
                onChangePeriod = onChangeProgressionPeriod,
                onExerciseClick = onNavigateToExerciseHistory,
            )
        }

        // Divider 3↔4
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Section 4 — Load Velocity
        item {
            Text(
                text = stringResource(R.string.metrics_load_velocity_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.loadVelocities) { velocity ->
            LoadVelocityRow(
                velocity = velocity,
                onClick = { onNavigateToExerciseHistory(velocity.exerciseId) },
            )
        }

        // Divider 4↔quick links
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
private fun AdherenceCard(adherence: AdherenceData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0E0E0),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metrics_adherence_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${"%.0f".format(adherence.percentage)}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.metrics_adherence_detail,
                    adherence.completedSessions,
                    adherence.plannedSessions,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RirByModuleCard(
    rirByModule: List<RirByModule>,
    onChangeRirPeriod: (Int) -> Unit,
) {
    val options = listOf(
        2 to stringResource(R.string.metrics_rir_2_sessions),
        4 to stringResource(R.string.metrics_rir_4_sessions),
        6 to stringResource(R.string.metrics_rir_6_sessions),
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options.first()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metrics_rir_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                TextField(
                    value = selectedOption.second,
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
                                selectedOption = option
                                expanded = false
                                onChangeRirPeriod(option.first)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            rirByModule.forEach { module ->
                RirModuleRow(module = module)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = stringResource(R.string.metrics_rir_reference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RirModuleRow(module: RirByModule) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Módulo ${module.moduleCode}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (module.averageRir == null || module.interpretation == null) {
            Text(
                text = stringResource(R.string.metrics_no_data),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val (badgeColor, badgeLabelColor, badgeText) = when (module.interpretation) {
                RirInterpretation.OPTIMAL -> Triple(
                    Color(0xFFE8F5E9),
                    Color(0xFF1B5E20),
                    stringResource(R.string.metrics_rir_optimal),
                )
                RirInterpretation.RISK_TOO_CLOSE -> Triple(
                    Color(0xFFFFDAD6),
                    Color(0xFF410002),
                    stringResource(R.string.metrics_rir_risk),
                )
                RirInterpretation.INSUFFICIENT_STIMULUS -> Triple(
                    Color(0xFFFFF8E1),
                    Color(0xFF5D4200),
                    stringResource(R.string.metrics_rir_insufficient),
                )
            }
            Text(
                text = "${"%.1f".format(module.averageRir)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Spacer(modifier = Modifier.width(8.dp))
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressionRateSection(
    rates: List<ExerciseProgressionRate>,
    onChangePeriod: (Int) -> Unit,
    onExerciseClick: (Long) -> Unit,
) {
    val options = listOf(
        4 to stringResource(R.string.metrics_period_4_weeks),
        8 to stringResource(R.string.metrics_period_8_weeks),
        12 to stringResource(R.string.metrics_period_12_weeks),
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options.first()) }

    Text(
        text = stringResource(R.string.metrics_progression_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = selectedOption.second,
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
                        selectedOption = option
                        expanded = false
                        onChangePeriod(option.first)
                    },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    rates.forEachIndexed { index, rate ->
        ProgressionRateRow(
            rate = rate,
            onClick = { onExerciseClick(rate.exerciseId) },
        )
        if (index < rates.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    if (rates.isEmpty()) {
        Text(
            text = stringResource(R.string.metrics_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressionRateRow(
    rate: ExerciseProgressionRate,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val (trendIcon, trendColor) = when {
        rate.rate >= 60 -> "↑" to if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
        rate.rate >= 40 -> "=" to if (isDark) Color(0xFFFFD54F) else Color(0xFF8D6E00)
        else -> "↓" to if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rate.exerciseName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${"%.0f".format(rate.rate)}%",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = trendIcon,
            color = trendColor,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun LoadVelocityRow(
    velocity: ExerciseLoadVelocity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = velocity.exerciseName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (velocity.isBodyweight) {
            Text(
                text = stringResource(R.string.metrics_load_velocity_na),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val formattedVelocity = when {
                velocity.velocity > 0 -> "+${"%.1f".format(velocity.velocity)} Kg/sesión"
                velocity.velocity < 0 -> "${"%.1f".format(velocity.velocity)} Kg/sesión"
                else -> "0 Kg/sesión"
            }
            Text(
                text = formattedVelocity,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
