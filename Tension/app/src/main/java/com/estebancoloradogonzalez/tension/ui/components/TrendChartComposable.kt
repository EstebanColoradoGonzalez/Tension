package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

data class TrendPoint(
    val label: String,
    val value: Float,
)

private val primaryChartColor = Color(0xFF8B1A1A)

@Composable
fun TrendChartComposable(
    data: List<TrendPoint>,
    yAxisLabel: String,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                drawTrendChart(
                    data = data,
                    yAxisLabel = yAxisLabel,
                    textMeasurer = textMeasurer,
                    gridColor = gridColor,
                    axisLabelColor = axisLabelColor,
                )
            }
        }
    }
}

private fun DrawScope.drawTrendChart(
    data: List<TrendPoint>,
    yAxisLabel: String,
    textMeasurer: TextMeasurer,
    gridColor: Color,
    axisLabelColor: Color,
) {
    val leftPadding = 60f
    val bottomPadding = 30f
    val topPadding = 16f
    val rightPadding = 20f

    val chartWidth = size.width - leftPadding - rightPadding
    val chartHeight = size.height - bottomPadding - topPadding

    val maxValue = data.maxOf { it.value }.coerceAtLeast(1f)
    val minValue = data.minOf { it.value }.coerceAtLeast(0f)
    val range = (maxValue - minValue).coerceAtLeast(1f)

    val labelStyle = TextStyle(
        fontSize = TextUnit(10f, TextUnitType.Sp),
        color = axisLabelColor,
    )

    // Draw horizontal grid lines
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
        val labelValue = minValue + range * i / gridLines
        val label = "%.0f".format(labelValue)
        val result = textMeasurer.measure(text = label, style = labelStyle)
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                leftPadding - result.size.width - 6f,
                y - result.size.height / 2f,
            ),
        )
    }

    // Draw Y axis label
    val yLabelResult = textMeasurer.measure(text = yAxisLabel, style = labelStyle)
    drawText(
        textLayoutResult = yLabelResult,
        topLeft = Offset(2f, topPadding - 2f),
    )

    // Draw axes
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

    // Compute points
    val xStep = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth / 2
    val points = data.mapIndexed { i, point ->
        val x = leftPadding + i * xStep
        val y = topPadding + chartHeight * (1 - (point.value - minValue) / range)
        Offset(x, y)
    }

    // Draw X axis labels
    data.forEachIndexed { i, point ->
        val x = leftPadding + i * xStep
        val result = textMeasurer.measure(text = point.label, style = labelStyle)
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                x - result.size.width / 2f,
                size.height - bottomPadding + 4f,
            ),
        )
    }

    // Draw connecting lines
    if (data.size >= 2) {
        for (i in 0 until points.size - 1) {
            drawLine(
                color = primaryChartColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
            )
        }
    }

    // Draw points
    points.forEach { point ->
        drawCircle(
            color = primaryChartColor,
            radius = 6.dp.toPx(),
            center = point,
        )
    }
}
