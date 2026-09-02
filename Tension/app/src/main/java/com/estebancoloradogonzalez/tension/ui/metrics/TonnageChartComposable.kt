package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot
import com.estebancoloradogonzalez.tension.ui.theme.TensionThemeExtended

/**
 * Tonnage evolution per microcycle.
 *
 * The chart declares what it measures: the Y axis carries the unit, the X axis carries
 * its meaning and every point is labelled as the microcycle it belongs to. Series and
 * axis colours come from the theme so the chart stays readable in both schemes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TonnageChartComposable(
    snapshots: List<TonnageSnapshot>,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) return

    val allGroups = snapshots.flatMap { it.tonnageByGroup.keys }.distinct().sorted()
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val themeSeriesColors = TensionThemeExtended.semanticColors.chartSeries
    val seriesColors = themeSeriesColors.ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisLabelColor)
    val yAxisTitle = stringResource(R.string.chart_axis_y_tonnage)
    val xAxisTitle = stringResource(R.string.chart_axis_x_microcycle)
    val xLabels = snapshots.map {
        stringResource(R.string.chart_microcycle_short_label, it.microcycleNumber)
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .height(CHART_HEIGHT)
                .horizontalScroll(rememberScrollState())
                .width((CHART_SIDE_WIDTH_DP + snapshots.size * CHART_POINT_WIDTH_DP).coerceAtLeast(240).dp),
        ) {
            drawChart(
                snapshots = snapshots,
                allGroups = allGroups,
                xLabels = xLabels,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                gridColor = gridColor,
                seriesColors = seriesColors,
                yAxisTitle = yAxisTitle,
                xAxisTitle = xAxisTitle,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            allGroups.forEachIndexed { index, group ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val seriesColor = seriesColors[index % seriesColors.size]
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = seriesColor,
                            radius = size.minDimension / 2,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.chart_legend_note),
            style = MaterialTheme.typography.bodySmall,
            color = axisLabelColor,
        )
    }
}

private fun DrawScope.drawChart(
    snapshots: List<TonnageSnapshot>,
    allGroups: List<String>,
    xLabels: List<String>,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    gridColor: Color,
    seriesColors: List<Color>,
    yAxisTitle: String,
    xAxisTitle: String,
) {
    val leftPadding = 96f
    val bottomPadding = 76f
    val topPadding = 40f
    val rightPadding = 24f

    val chartWidth = size.width - leftPadding - rightPadding
    val chartHeight = size.height - bottomPadding - topPadding

    val maxTonnage = snapshots.flatMap { it.tonnageByGroup.values }.maxOrNull()?.coerceAtLeast(1.0)
        ?: 1.0

    // Y axis title carries the unit of every value on the chart
    val yTitleResult = textMeasurer.measure(text = yAxisTitle, style = labelStyle)
    drawText(
        textLayoutResult = yTitleResult,
        topLeft = Offset(4f, topPadding - yTitleResult.size.height - 6f),
    )

    // Grid lines with their value on the Y axis
    val gridLines = 4
    for (i in 0..gridLines) {
        val y = topPadding + chartHeight * (1 - i.toFloat() / gridLines)
        drawLine(
            color = gridColor,
            start = Offset(leftPadding, y),
            end = Offset(size.width - rightPadding, y),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)),
            strokeWidth = 1f,
        )
        val label = "%.0f".format(maxTonnage * i / gridLines)
        val result = textMeasurer.measure(text = label, style = labelStyle)
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                leftPadding - result.size.width - 8f,
                y - result.size.height / 2f,
            ),
        )
    }

    // Axes
    drawLine(
        color = gridColor,
        start = Offset(leftPadding, topPadding),
        end = Offset(leftPadding, size.height - bottomPadding),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = gridColor,
        start = Offset(leftPadding, size.height - bottomPadding),
        end = Offset(size.width - rightPadding, size.height - bottomPadding),
        strokeWidth = 1.5f,
    )

    // X axis title states what each point is
    val xTitleResult = textMeasurer.measure(text = xAxisTitle, style = labelStyle)
    drawText(
        textLayoutResult = xTitleResult,
        topLeft = Offset(
            leftPadding + (chartWidth - xTitleResult.size.width) / 2f,
            size.height - xTitleResult.size.height - 4f,
        ),
    )

    if (snapshots.size < 2) return

    val xStep = chartWidth / (snapshots.size - 1).coerceAtLeast(1)

    // X labels
    snapshots.forEachIndexed { i, _ ->
        val x = leftPadding + i * xStep
        val result = textMeasurer.measure(text = xLabels[i], style = labelStyle)
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                x - result.size.width / 2f,
                size.height - bottomPadding + 8f,
            ),
        )
    }

    // One line per muscle group
    allGroups.forEachIndexed { groupIndex, group ->
        val color = seriesColors[groupIndex % seriesColors.size]
        val points = snapshots.mapIndexed { i, snapshot ->
            val x = leftPadding + i * xStep
            val tonnage = snapshot.tonnageByGroup[group] ?: 0.0
            val y = topPadding + chartHeight * (1 - (tonnage / maxTonnage)).toFloat()
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
            )
        }

        points.forEach { point ->
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = point,
            )
        }
    }
}

private val CHART_HEIGHT = 220.dp
private const val CHART_SIDE_WIDTH_DP = 100
private const val CHART_POINT_WIDTH_DP = 68
