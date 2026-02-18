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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot

private val chartColors = listOf(
    Color(0xFF8B1A1A), // Primary
    Color(0xFF6B4F4F), // Secondary
    Color(0xFF5C6B4F), // Tertiary
    Color(0xFF1A5C8B), // Blue
    Color(0xFF8B6B1A), // Gold
    Color(0xFF4F1A8B), // Purple
    Color(0xFF1A8B6B), // Teal
    Color(0xFF8B1A6B), // Magenta
    Color(0xFF4F8B1A), // Lime
    Color(0xFF1A4F8B), // Navy
    Color(0xFF8B4F1A), // Brown
    Color(0xFF6B1A8B), // Violet
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TonnageChartComposable(
    snapshots: List<TonnageSnapshot>,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) return

    val allGroups = snapshots.flatMap { it.tonnageByGroup.keys }.distinct().sorted()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .height(200.dp)
                .horizontalScroll(rememberScrollState())
                .width((80 + snapshots.size * 60).coerceAtLeast(200).dp),
        ) {
            drawChart(
                snapshots = snapshots,
                allGroups = allGroups,
                textMeasurer = textMeasurer,
                gridColor = gridColor,
                axisLabelColor = axisLabelColor,
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
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = chartColors[index % chartColors.size],
                            radius = size.minDimension / 2,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group,
                        style = labelStyle,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawChart(
    snapshots: List<TonnageSnapshot>,
    allGroups: List<String>,
    textMeasurer: TextMeasurer,
    gridColor: Color,
    axisLabelColor: Color,
) {
    val leftPadding = 60f
    val bottomPadding = 30f
    val topPadding = 10f
    val rightPadding = 20f

    val chartWidth = size.width - leftPadding - rightPadding
    val chartHeight = size.height - bottomPadding - topPadding

    val maxTonnage = snapshots.flatMap { it.tonnageByGroup.values }.maxOrNull()?.coerceAtLeast(1.0)
        ?: 1.0

    // Draw grid lines
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
        val result = textMeasurer.measure(
            text = label,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp,
                color = axisLabelColor,
            ),
        )
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                leftPadding - result.size.width - 6f,
                y - result.size.height / 2f,
            ),
        )
    }

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

    if (snapshots.size < 2) return

    val xStep = chartWidth / (snapshots.size - 1).coerceAtLeast(1)

    // Draw X labels
    snapshots.forEachIndexed { i, snapshot ->
        val x = leftPadding + i * xStep
        val label = "${snapshot.microcycleNumber}"
        val result = textMeasurer.measure(
            text = label,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp,
                color = axisLabelColor,
            ),
        )
        drawText(
            textLayoutResult = result,
            topLeft = Offset(
                x - result.size.width / 2f,
                size.height - bottomPadding + 4f,
            ),
        )
    }

    // Draw lines per group
    allGroups.forEachIndexed { groupIndex, group ->
        val color = chartColors[groupIndex % chartColors.size]
        val points = snapshots.mapIndexed { i, snapshot ->
            val x = leftPadding + i * xStep
            val tonnage = snapshot.tonnageByGroup[group] ?: 0.0
            val y = topPadding + chartHeight * (1 - (tonnage / maxTonnage)).toFloat()
            Offset(x, y)
        }

        // Lines
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2f,
            )
        }

        // Points
        points.forEach { point ->
            drawCircle(
                color = color,
                radius = 4f,
                center = point,
            )
        }
    }
}

private val Int.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
