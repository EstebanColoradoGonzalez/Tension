package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTrend
import com.estebancoloradogonzalez.tension.domain.model.TrendDirection
import com.estebancoloradogonzalez.tension.ui.components.EntityNameText
import com.estebancoloradogonzalez.tension.ui.components.MetricInsufficientBlock
import com.estebancoloradogonzalez.tension.ui.components.MetricListCard
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirement
import com.estebancoloradogonzalez.tension.ui.components.MetricSectionHeader
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.trend_title),
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
            is TrendUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is TrendUiState.Error -> {
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
            is TrendUiState.InsufficientData -> {
                TrendContent(
                    trends = emptyList(),
                    evaluatedMicrocycles = MetricSufficiencyRules.MIN_TREND_MICROCYCLES,
                    requirement = state.requirement,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            is TrendUiState.Content -> {
                TrendContent(
                    trends = state.trends,
                    evaluatedMicrocycles = state.evaluatedMicrocycles,
                    requirement = null,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun TrendContent(
    trends: List<MuscleGroupTrend>,
    evaluatedMicrocycles: Int,
    requirement: MetricRequirement?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item { MetricSectionHeader(title = stringResource(R.string.trend_section_muscle_groups)) }

        item {
            MetricListCard(
                label = stringResource(R.string.trend_muscle_group_title),
                description = stringResource(R.string.trend_description),
                period = stringResource(R.string.trend_period_microcycles, evaluatedMicrocycles),
            ) {
                if (requirement != null) {
                    MetricInsufficientBlock(requirement = requirement)
                } else {
                    trends.forEach { trend ->
                        TrendRow(trend = trend)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrendRow(trend: MuscleGroupTrend) {
    val semanticColors = TensionThemeExtended.semanticColors

    val (label, icon, color) = when (trend.direction) {
        TrendDirection.ASCENDING -> Triple(
            stringResource(R.string.trend_ascending),
            ICON_ASCENDING,
            semanticColors.trendAscending,
        )
        TrendDirection.STABLE -> Triple(
            stringResource(R.string.trend_stable),
            ICON_STABLE,
            semanticColors.trendStable,
        )
        TrendDirection.DECLINING -> Triple(
            stringResource(R.string.trend_declining),
            ICON_DECLINING,
            semanticColors.trendDeclining,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityNameText(
            text = trend.muscleGroup,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = icon)
    }
}

private const val ICON_ASCENDING = "📈"
private const val ICON_STABLE = "📊"
private const val ICON_DECLINING = "📉"
