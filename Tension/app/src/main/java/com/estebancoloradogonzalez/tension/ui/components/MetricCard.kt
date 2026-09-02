package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended

/**
 * Presentation of the analytics indicators, shared by the three screens of Flow G.
 *
 * Every indicator is rendered as a card composing, in this order, its label, its value
 * with its unit, a short description of what it represents and the period it was
 * computed over. Keeping the anatomy in a single file is what makes that order a
 * property of the system instead of a convention repeated three times.
 */
private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val CARD_PADDING = 16.dp
private val ROW_MIN_HEIGHT = 48.dp

/** Thematic header that opens a group of indicator cards. */
@Composable
fun MetricSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Indicator whose value is a single scalar. The value is the typographically dominant
 * element of the card; when it cannot be computed, the card states what is missing
 * instead of showing a zero.
 */
@Composable
fun MetricCard(
    label: String,
    value: MetricValue,
    description: String,
    period: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MetricValueText(value = value)
            Spacer(modifier = Modifier.height(8.dp))
            if (value is MetricValue.Insufficient) {
                Text(
                    text = metricRequirementText(value.requirement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = period,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Lays out one or two [MetricCard] side by side, both stretched to the tallest of the
 * pair. A single card keeps half the width so the grid does not break.
 */
@Composable
fun MetricCardPair(
    left: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    right: (@Composable (Modifier) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        left(Modifier.weight(1f).fillMaxHeight())
        if (right != null) {
            right(Modifier.weight(1f).fillMaxHeight())
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Indicator whose value is a series over entities — exercises, routines or muscle
 * groups. The card carries the label, the description and the period; each row carries
 * its entity and its own value with its unit.
 */
@Composable
fun MetricListCard(
    label: String,
    description: String,
    period: String,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (header != null) {
                Spacer(modifier = Modifier.height(8.dp))
                header()
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = period,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One entity of a [MetricListCard]: its name and its value. When the value cannot be
 * computed, the row states what is missing right under the name — the marker alone
 * would be as mute as the zero it replaces.
 */
@Composable
fun MetricEntityRow(
    name: String,
    value: MetricValue,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            EntityNameText(
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (value is MetricValue.Insufficient) {
                Text(
                    text = metricRequirementText(value.requirement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        MetricValueText(
            value = value,
            valueStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            unitStyle = MaterialTheme.typography.bodySmall,
            showRequirement = false,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Renders the three states of a [MetricValue]. A computed zero reaches the
 * [MetricValue.Available] branch with the value typography; an absent datum never
 * does, which is what keeps both states distinguishable.
 */
@Composable
fun MetricValueText(
    value: MetricValue,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    unitStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showRequirement: Boolean = true,
) {
    when (value) {
        is MetricValue.Available -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = MetricFormatRules.formatAmount(value.amount, value.unit),
                    style = valueStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val symbol = metricUnitSymbol(value.unit)
                if (symbol != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = symbol,
                        style = unitStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        is MetricValue.Insufficient -> MetricInsufficientBlock(
            requirement = value.requirement,
            modifier = modifier,
            markerStyle = valueStyle,
            showRequirement = showRequirement,
        )
        MetricValue.NotApplicable -> Text(
            text = stringResource(R.string.metric_not_applicable),
            style = unitStyle.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

/**
 * Neutral marker plus the explicit sentence of what is missing. Never a zero, never a
 * bare dash.
 */
@Composable
fun MetricInsufficientBlock(
    requirement: MetricRequirement,
    modifier: Modifier = Modifier,
    markerStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    showRequirement: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.metric_insufficient_marker),
            style = markerStyle,
            color = TensionThemeExtended.semanticColors.metricInsufficient,
        )
        if (showRequirement) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metricRequirementText(requirement),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Sentence stating what the indicator is missing in order to be computable. */
@Composable
fun metricRequirementText(requirement: MetricRequirement): String = when (requirement.kind) {
    MetricRequirementKind.EXERCISE_OBSERVATIONS ->
        stringResource(R.string.metric_insufficient_exercise_observations)
    MetricRequirementKind.EXERCISE_SESSIONS ->
        stringResource(R.string.metric_insufficient_exercise_sessions, requirement.missing)
    MetricRequirementKind.ROUTINE_SETS ->
        stringResource(R.string.metric_insufficient_routine_sets)
    MetricRequirementKind.WEEKLY_TARGET ->
        stringResource(R.string.metric_insufficient_weekly_target)
    MetricRequirementKind.MICROCYCLE_SESSIONS ->
        stringResource(R.string.metric_insufficient_microcycle_sessions)
    MetricRequirementKind.COMPLETE_MICROCYCLES ->
        stringResource(R.string.metric_insufficient_complete_microcycles, requirement.missing)
}

/** Symbol of the presentation unit, or null when the indicator is a bare count. */
@Composable
fun metricUnitSymbol(unit: MetricUnit): String? = when (unit) {
    MetricUnit.KILOGRAM -> stringResource(R.string.metric_unit_kg)
    MetricUnit.KILOGRAM_PER_SESSION -> stringResource(R.string.metric_unit_kg_per_session)
    MetricUnit.PERCENTAGE -> stringResource(R.string.metric_unit_percentage)
    MetricUnit.RIR -> stringResource(R.string.metric_unit_rir)
    MetricUnit.COUNT -> null
}
