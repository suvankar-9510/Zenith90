package com.zenith90.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.DailyLogEntity
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState

@Composable
fun AnalyticsScreen(state: ZenithUiState) {
    val logs = state.fourMonthHistory

    val studyHoursPoints = if (logs.size >= 5) {
        logs.takeLast(14).map { (it.totalStudyMinutes / 60f).coerceIn(0f, 12f) }
    } else {
        listOf(4.5f, 6.0f, 5.5f, 7.2f, 6.8f, 8.0f, 7.5f, 6.2f, 8.4f, 9.0f)
    }

    val wakeDeviationPoints = if (logs.size >= 5) {
        logs.takeLast(14).map { (it.wakeDeviationMinutes?.toFloat() ?: 0f).coerceIn(-30f, 60f) }
    } else {
        listOf(15f, 10f, 0f, -5f, 0f, 5f, 0f, -10f, 0f, 0f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "OCTOBER — JANUARY TRAJECTORY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "4-Month Transformation Telemetry",
                style = MaterialTheme.typography.displayMedium
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "DEEP STUDY HOURS VELOCITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Daily Logged Hours (Bezier Trend)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(20.dp))
                    SmoothBezierGradientChart(
                        dataPoints = studyHoursPoints,
                        maxValue = 10f,
                        minValue = 0f,
                        lineBrush = ZenithColors.StudyCompletionGradient,
                        areaBrush = ZenithColors.studyVerticalAreaBrush(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CIRCADIAN ALIGNMENT (TARGET DEVIATION)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Minutes Relative to 05:30 AM Target",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(20.dp))
                    SmoothBezierGradientChart(
                        dataPoints = wakeDeviationPoints,
                        maxValue = 60f,
                        minValue = -30f,
                        lineBrush = ZenithColors.StreakGradient,
                        areaBrush = ZenithColors.wakeVerticalAreaBrush(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
        }

        item { HabitHeatmapCard(logs = logs) }
    }
}

@Composable
fun SmoothBezierGradientChart(
    dataPoints: List<Float>,
    maxValue: Float,
    minValue: Float,
    lineBrush: androidx.compose.ui.graphics.Brush,
    areaBrush: androidx.compose.ui.graphics.Brush,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (dataPoints.size < 2) return@Canvas

        val range = (maxValue - minValue).coerceAtLeast(1f)
        val stepX = size.width / (dataPoints.size - 1)

        val coordinates = dataPoints.mapIndexed { index, value ->
            val normalizedY = ((value - minValue) / range).coerceIn(0f, 1f)
            val x = index * stepX
            val y = size.height - (normalizedY * size.height * 0.85f) - (size.height * 0.08f)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            for (i in 0 until coordinates.size - 1) {
                val p0 = coordinates[i]
                val p1 = coordinates[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(coordinates.last().x, size.height)
            lineTo(coordinates.first().x, size.height)
            close()
        }

        drawPath(path = fillPath, brush = areaBrush)
        drawPath(
            path = strokePath,
            brush = lineBrush,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun HabitHeatmapCard(logs: List<DailyLogEntity>) {
    val logMap = logs.associateBy { it.dateEpochDay }
    val baseEpoch = java.time.LocalDate.of(2026, 10, 1).toEpochDay()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "120-DAY CONSISTENCY MATRIX (OCT – JAN)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Daily 100% Pillar Execution Heatmap",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (week in 0 until 17) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (day in 0 until 7) {
                            val epoch = baseEpoch + (week * 7) + day
                            val ratio = logMap[epoch]?.completionRatio ?: 0f
                            val cellColor = when {
                                ratio >= 0.9f -> ZenithColors.AmberStart
                                ratio >= 0.6f -> ZenithColors.CoralEnd.copy(alpha = 0.75f)
                                ratio >= 0.25f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
