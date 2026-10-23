package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.WeeklyTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Body weight and weekly tonnage on the same time axis (CA-48.03).
 *
 * The two series share the X axis —dates of the selected period— and nothing else: each one
 * keeps its own scale (weight on the left, tonnage on the right), because a few tenths of a
 * kilogram of body weight and thousands of kilograms moved cannot share a Y axis.
 *
 * - **Weight:** a line with one point per entry of the history.
 * - **Tonnage:** one bar per ISO week with sessions, centred on the middle of its week. A week
 *   without sessions has no bar: an absent value is never drawn as a zero (HU-35).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyWeightChartComposable(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    weightPoints: List<WeightRecord>,
    weeklyTonnage: List<WeeklyTonnage>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val seriesColors = TensionThemeExtended.semanticColors.chartSeries
    val fallbackWeightColor = MaterialTheme.colorScheme.primary
    val fallbackTonnageColor = MaterialTheme.colorScheme.tertiary
    val weightColor = seriesColors.getOrElse(0) { fallbackWeightColor }
    val tonnageColor = seriesColors.getOrElse(1) { fallbackTonnageColor }.copy(alpha = TONNAGE_BAR_ALPHA)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisLabelColor)
    val weightAxisTitle = stringResource(R.string.chart_axis_y_body_weight)
    val tonnageAxisTitle = stringResource(R.string.chart_axis_y_weekly_tonnage)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        ) {
            drawBodyWeightChart(
                axis = TimeAxis(periodStart, periodEnd),
                weightPoints = weightPoints,
                weeklyTonnage = weeklyTonnage,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                gridColor = gridColor,
                weightColor = weightColor,
                tonnageColor = tonnageColor,
                weightAxisTitle = weightAxisTitle,
                tonnageAxisTitle = tonnageAxisTitle,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LegendEntry(
                label = stringResource(R.string.chart_legend_body_weight),
                color = weightColor,
                isBar = false,
            )
            LegendEntry(
                label = stringResource(R.string.chart_legend_weekly_tonnage),
                color = tonnageColor,
                isBar = true,
            )
        }

        if (weeklyTonnage.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.metrics_body_weight_no_tonnage),
                style = MaterialTheme.typography.bodySmall,
                color = axisLabelColor,
            )
        }
    }
}

@Composable
private fun LegendEntry(
    label: String,
    color: Color,
    isBar: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            if (isBar) {
                drawRect(color = color)
            } else {
                drawCircle(color = color, radius = size.minDimension / 2)
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Maps a date of the period to its fraction of the width: 0 at the start, 1 at the end. */
private class TimeAxis(private val start: LocalDate, end: LocalDate) {
    private val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)

    fun fraction(date: LocalDate): Float =
        (ChronoUnit.DAYS.between(start, date).toFloat() / totalDays).coerceIn(0f, 1f)

    fun dayWidth(): Float = 1f / totalDays

    val labels: List<LocalDate> = listOf(start, start.plusDays(totalDays / 2), end)
}

private fun DrawScope.drawBodyWeightChart(
    axis: TimeAxis,
    weightPoints: List<WeightRecord>,
    weeklyTonnage: List<WeeklyTonnage>,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    gridColor: Color,
    weightColor: Color,
    tonnageColor: Color,
    weightAxisTitle: String,
    tonnageAxisTitle: String,
) {
    val left = 88f
    val right = 104f
    val top = 40f
    val bottom = 48f
    val chartWidth = size.width - left - right
    val chartHeight = size.height - top - bottom
    val baseline = top + chartHeight

    fun xOf(date: LocalDate): Float = left + chartWidth * axis.fraction(date)

    // Axis titles carry the unit of each scale
    val weightTitle = textMeasurer.measure(text = weightAxisTitle, style = labelStyle)
    drawText(weightTitle, topLeft = Offset(4f, top - weightTitle.size.height - 8f))
    val tonnageTitle = textMeasurer.measure(text = tonnageAxisTitle, style = labelStyle)
    drawText(
        tonnageTitle,
        topLeft = Offset(size.width - tonnageTitle.size.width - 4f, top - tonnageTitle.size.height - 8f),
    )

    // Weight scale: a band around the observed values, so tenths of a kilogram stay visible
    val weights = weightPoints.map { it.weightKg }
    val weightMin = (weights.minOrNull() ?: 0.0) - WEIGHT_SCALE_MARGIN_KG
    val weightMax = (weights.maxOrNull() ?: 0.0) + WEIGHT_SCALE_MARGIN_KG
    val tonnageMax = weeklyTonnage.maxOfOrNull { it.tonnageKg }?.coerceAtLeast(1.0) ?: 1.0

    val gridLines = 4
    for (i in 0..gridLines) {
        val ratio = i.toDouble() / gridLines
        val y = baseline - chartHeight * ratio.toFloat()
        drawLine(
            color = gridColor,
            start = Offset(left, y),
            end = Offset(left + chartWidth, y),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)),
            strokeWidth = 1f,
        )
        if (weights.isNotEmpty()) {
            val label = textMeasurer.measure(
                text = "%.1f".format(weightMin + (weightMax - weightMin) * ratio),
                style = labelStyle,
            )
            drawText(label, topLeft = Offset(left - label.size.width - 8f, y - label.size.height / 2f))
        }
        if (weeklyTonnage.isNotEmpty()) {
            val label = textMeasurer.measure(text = "%.0f".format(tonnageMax * ratio), style = labelStyle)
            drawText(label, topLeft = Offset(left + chartWidth + 8f, y - label.size.height / 2f))
        }
    }

    drawLine(gridColor, Offset(left, top), Offset(left, baseline), strokeWidth = 1.5f)
    drawLine(gridColor, Offset(left, baseline), Offset(left + chartWidth, baseline), strokeWidth = 1.5f)

    axis.labels.forEach { date ->
        val label = textMeasurer.measure(text = date.format(SHORT_DATE), style = labelStyle)
        val x = (xOf(date) - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
        drawText(label, topLeft = Offset(x, baseline + 8f))
    }

    // Tonnage: one bar per week with sessions, centred on the middle of the week
    val barWidth = (chartWidth * axis.dayWidth() * BAR_WIDTH_DAYS).coerceAtLeast(4f)
    weeklyTonnage.forEach { week ->
        val centre = xOf(week.weekStart.plusDays(MID_WEEK_OFFSET_DAYS))
        val barHeight = chartHeight * (week.tonnageKg / tonnageMax).toFloat()
        drawRect(
            color = tonnageColor,
            topLeft = Offset(centre - barWidth / 2f, baseline - barHeight),
            size = Size(barWidth, barHeight),
        )
    }

    // Weight: one point per entry, joined in chronological order
    val points = weightPoints.map { record ->
        val ratio = (record.weightKg - weightMin) / (weightMax - weightMin)
        Offset(xOf(record.date), baseline - chartHeight * ratio.toFloat())
    }
    points.zipWithNext { from, to ->
        drawLine(color = weightColor, start = from, end = to, strokeWidth = 2.dp.toPx())
    }
    points.forEach { point ->
        drawCircle(color = weightColor, radius = 4.dp.toPx(), center = point)
    }
}

private val CHART_HEIGHT = 220.dp
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
private const val WEIGHT_SCALE_MARGIN_KG = 1.0
private const val TONNAGE_BAR_ALPHA = 0.55f
private const val BAR_WIDTH_DAYS = 4f
private const val MID_WEEK_OFFSET_DAYS = 3L
