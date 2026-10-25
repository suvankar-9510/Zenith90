package com.zenith90.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.DailyLogEntity
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate

@Composable
fun AnalyticsScreen(state: ZenithUiState) {
    val logs = state.fourMonthHistory
    val todayEpoch = state.todayDate.toEpochDay()
    val logMap = logs.associateBy { it.dateEpochDay }

    // Real dynamic 14-day rolling points (no hardcoded fake values)
    val past14Epochs = (13 downTo 0).map { todayEpoch - it }
    val studyHoursPoints = past14Epochs.map { epoch ->
        val log = logMap[epoch]
        if (log != null) (log.totalStudyMinutes / 60f).coerceIn(0f, 16f) else 0f
    }

    val wakeDeviationPoints = past14Epochs.map { epoch ->
        val log = logMap[epoch]
        if (log != null && log.actualWakeTimeMinutes != null) {
            (log.wakeDeviationMinutes?.toFloat() ?: 0f).coerceIn(-45f, 90f)
        } else 0f
    }

    // Real aggregate metrics
    val totalLoggedStudyMinutes = logs.sumOf { it.totalStudyMinutes }
    val loggedDaysCount = logs.count { it.totalStudyMinutes > 0 || it.actualWakeTimeMinutes != null || it.workoutCompleted }
    val avgDailyStudyHours = if (loggedDaysCount > 0) (totalLoggedStudyMinutes / 60f) / loggedDaysCount else 0f
    val perfectDaysCount = logs.count { it.completionRatio >= 0.99f }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 100.dp)
    ) {
        item {
            Column {
                Text(
                    text = "HISTORICAL TELEMETRY & TRAJECTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.examTitle} Readiness",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Telemetry dynamically evaluated from Room database records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Metric Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "TOTAL STUDY",
                    value = "${totalLoggedStudyMinutes / 60}h ${totalLoggedStudyMinutes % 60}m",
                    subtitle = "Across all logs",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "AVG DAILY VELOCITY",
                    value = "${"%.1f".format(avgDailyStudyHours)}h",
                    subtitle = "Per active day",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "PERFECT DAYS",
                    value = "$perfectDaysCount",
                    subtitle = "100% 3-pillar completion",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real Study Hours Velocity Bezier Chart
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "14-DAY STUDY HOURS VELOCITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Daily Hours (Actual Room Telemetry)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Peak: ${"%.1f".format(studyHoursPoints.maxOrNull() ?: 0f)}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenithColors.SageStart,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    SmoothBezierGradientChart(
                        dataPoints = studyHoursPoints,
                        maxValue = (studyHoursPoints.maxOrNull() ?: 6f).coerceAtLeast(6f),
                        minValue = 0f,
                        lineBrush = ZenithColors.StudyCompletionGradient,
                        areaBrush = ZenithColors.studyVerticalAreaBrush(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                }
            }
        }

        // Real Circadian Alignment Chart
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "14-DAY CIRCADIAN WAKE DEVIATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Minutes Relative to Target Wake Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(18.dp))
                    SmoothBezierGradientChart(
                        dataPoints = wakeDeviationPoints,
                        maxValue = 60f,
                        minValue = -30f,
                        lineBrush = ZenithColors.StreakGradient,
                        areaBrush = ZenithColors.wakeVerticalAreaBrush(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }

        // Real 120-Day Heatmap Grid
        item {
            HabitHeatmapCard(logs = logs, todayEpoch = todayEpoch)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
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
private fun HabitHeatmapCard(logs: List<DailyLogEntity>, todayEpoch: Long) {
    val logMap = logs.associateBy { it.dateEpochDay }
    // Start window: 119 days ago (17 weeks of 7 days)
    val baseEpoch = todayEpoch - 118

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "120-DAY DISCIPLINE MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Dynamic 3-Pillar Execution Grid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${logs.count { it.completionRatio > 0f }} Active Days",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenithColors.AmberStart,
                    fontWeight = FontWeight.Bold
                )
            }

            // 17 weeks x 7 days grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (week in 0 until 17) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (day in 0 until 7) {
                            val epoch = baseEpoch + (week * 7) + day
                            val log = logMap[epoch]
                            val ratio = log?.completionRatio ?: 0f
                            val isToday = epoch == todayEpoch
                            val isFuture = epoch > todayEpoch

                            val cellColor = when {
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ratio >= 0.9f -> ZenithColors.AmberStart
                                ratio >= 0.6f -> ZenithColors.CoralEnd.copy(alpha = 0.85f)
                                ratio >= 0.25f -> ZenithColors.SageStart.copy(alpha = 0.65f)
                                log != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(cellColor)
                                    .then(
                                        if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.5.dp))
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(ZenithColors.SageStart.copy(alpha = 0.65f)))
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(ZenithColors.CoralEnd.copy(alpha = 0.85f)))
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(ZenithColors.AmberStart))
                Text(text = "100% Equilibrium", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
