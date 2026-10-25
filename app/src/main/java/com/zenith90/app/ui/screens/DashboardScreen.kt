package com.zenith90.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.auth.ZenithUser
import com.zenith90.app.data.local.*
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState

@Composable
fun DashboardScreen(
    state: ZenithUiState,
    onLogWakeUpNow: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onSwitchWorkoutModality: (WorkoutModality) -> Unit,
    onToggleWorkoutCompleted: (Boolean) -> Unit,
    onToggleBlockCompletion: (ScheduleBlockEntity) -> Unit,
    onRescheduleBlockTomorrow: (ScheduleBlockEntity) -> Unit,
    onOpenNightBlueprintSheet: () -> Unit,
    onSignOut: () -> Unit
) {
    val view = LocalView.current
    var showProfileDialog by remember { mutableStateOf(false) }

    val dayLog = state.currentDaySnapshot?.dailyLog ?: DailyLogEntity(
        userId = state.currentUser?.uid ?: "guest",
        dateEpochDay = state.todayDate.toEpochDay()
    )
    val blocks = state.currentDaySnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            ExamCountdownHeader(
                currentUser = state.currentUser,
                daysRemaining = state.daysRemaining,
                readinessPercent = state.transformationReadinessPercent,
                streakDays = state.overallStreakDays,
                onOpenBlueprint = {
                    PixelHaptics.crispTick(view)
                    onOpenNightBlueprintSheet()
                },
                onOpenProfile = {
                    PixelHaptics.crispTick(view)
                    showProfileDialog = true
                }
            )
        }

        item { ThreePillarRingCard(dayLog = dayLog) }

        item {
            WakeUpTrackerCard(
                targetMinutes = dayLog.targetWakeTimeMinutes,
                actualMinutes = dayLog.actualWakeTimeMinutes,
                onLogWakeNow = {
                    PixelHaptics.confirmCheck(view)
                    onLogWakeUpNow()
                }
            )
        }

        item {
            StudyFocusMonitorCard(
                loggedMinutes = dayLog.totalStudyMinutes,
                targetMinutes = dayLog.targetStudyMinutes,
                isTimerRunning = state.isFocusTimerRunning,
                elapsedSeconds = state.activeFocusSeconds,
                onToggleTimer = {
                    PixelHaptics.confirmCheck(view)
                    onToggleFocusTimer()
                }
            )
        }

        item {
            TrainingPillarCard(
                modality = dayLog.workoutModality,
                isCompleted = dayLog.workoutCompleted,
                onSwitchModality = {
                    PixelHaptics.crispTick(view)
                    onSwitchWorkoutModality(it)
                },
                onToggleComplete = {
                    PixelHaptics.heavyUnlock(view)
                    onToggleWorkoutCompleted(it)
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TIME-BLOCKED BLUEPRINT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    PixelHaptics.crispTick(view)
                    onOpenNightBlueprintSheet()
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("NIGHT PLANNER", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (blocks.isEmpty()) {
            item { EmptyBlueprintPrompt(onOpenNightBlueprintSheet) }
        } else {
            items(blocks, key = { it.id }) { block ->
                ScheduleBlockItemCard(
                    block = block,
                    onToggle = {
                        PixelHaptics.confirmCheck(view)
                        onToggleBlockCompletion(block)
                    },
                    onRollOverTomorrow = {
                        PixelHaptics.crispTick(view)
                        onRescheduleBlockTomorrow(block)
                    }
                )
            }
        }
    }

    if (showProfileDialog) {
        UserProfileDialog(
            user = state.currentUser,
            onDismiss = { showProfileDialog = false },
            onSignOut = {
                showProfileDialog = false
                PixelHaptics.heavyUnlock(view)
                onSignOut()
            }
        )
    }
}

@Composable
private fun ExamCountdownHeader(
    currentUser: ZenithUser?,
    daysRemaining: Long,
    readinessPercent: Float,
    streakDays: Int,
    onOpenBlueprint: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ZenithColors.SageStart)
                    )
                    Text(
                        text = "JAN EXAM • $daysRemaining DAYS LEFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { onOpenBlueprint() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 $streakDays D",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Profile Avatar Chip
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, ZenithColors.AmberStart),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onOpenProfile() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.initials ?: "ZN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "TRANSFORMATION READINESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentUser?.displayName ?: "Zenith Trajectory",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${"%.1f".format(readinessPercent)}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((readinessPercent / 100f).coerceIn(0.04f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(ZenithColors.StudyCompletionGradient)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileDialog(
    user: ZenithUser?,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ZenithColors.StreakGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.initials ?: "ZN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F0F12)
                    )
                }
                Column {
                    Text(
                        text = user?.displayName ?: "Zenith Scholar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (user?.isAnonymous == true) "Guest Session" else (user?.email ?: "Offline Vault"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account UID:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = user?.uid?.take(16) ?: "guest",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auth Provider:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (user?.isAnonymous == true) "Guest/Anonymous" else "Firebase Auth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Exam:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("20 Jan 2027", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SIGN OUT", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}

@Composable
private fun ThreePillarRingCard(dayLog: DailyLogEntity) {
    val studyProgress by animateFloatAsState(
        targetValue = (dayLog.totalStudyMinutes.toFloat() / dayLog.targetStudyMinutes.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "studyRing"
    )
    val wakeProgress by animateFloatAsState(
        targetValue = if (dayLog.isWakeGoalMet) 1f else if (dayLog.actualWakeTimeMinutes != null) 0.5f else 0f,
        animationSpec = tween(900),
        label = "wakeRing"
    )
    val workoutProgress by animateFloatAsState(
        targetValue = if (dayLog.workoutCompleted) 1f else 0f,
        animationSpec = tween(900),
        label = "workoutRing"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "3-PILLAR DAILY EQUILIBRIUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(dayLog.completionRatio * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PillarLegendRow("WAKE TARGET", formatMinutesToTime(dayLog.targetWakeTimeMinutes), ZenithColors.AmberStart)
                PillarLegendRow("DEEP STUDY", "${dayLog.totalStudyMinutes / 60}h ${dayLog.totalStudyMinutes % 60}m / 6h", ZenithColors.SageStart)
                PillarLegendRow("TRAINING", if (dayLog.workoutCompleted) "LOCKED IN" else "PENDING", ZenithColors.VioletStart)
            }

            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val spacing = 15.dp.toPx()

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = ZenithColors.StudyCompletionGradient,
                        startAngle = -90f,
                        sweepAngle = 360f * studyProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val midInset = spacing
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset),
                        size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = ZenithColors.StreakGradient,
                        startAngle = -90f,
                        sweepAngle = 360f * wakeProgress,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset),
                        size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val innerInset = spacing * 2
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(innerInset, innerInset),
                        size = androidx.compose.ui.geometry.Size(size.width - innerInset * 2, size.height - innerInset * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ZenithColors.VioletStart,
                        startAngle = -90f,
                        sweepAngle = 360f * workoutProgress,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(innerInset, innerInset),
                        size = androidx.compose.ui.geometry.Size(size.width - innerInset * 2, size.height - innerInset * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillarLegendRow(label: String, value: String, dotColor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(text = "$label:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WakeUpTrackerCard(
    targetMinutes: Int,
    actualMinutes: Int?,
    onLogWakeNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PILLAR I • CIRCADIAN WAKE-UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (actualMinutes != null) {
                        "Logged: ${formatMinutesToTime(actualMinutes)} (Target ${formatMinutesToTime(targetMinutes)})"
                    } else {
                        "Target Window: ${formatMinutesToTime(targetMinutes)}"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            FilledTonalButton(
                onClick = onLogWakeNow,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (actualMinutes == null) "LOG WAKE" else "UPDATE", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun StudyFocusMonitorCard(
    loggedMinutes: Int,
    targetMinutes: Int,
    isTimerRunning: Boolean,
    elapsedSeconds: Int,
    onToggleTimer: () -> Unit
) {
    val mm = (elapsedSeconds / 60).toString().padStart(2, '0')
    val ss = (elapsedSeconds % 60).toString().padStart(2, '0')

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PILLAR II • DEEP STUDY ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${loggedMinutes / 60}h ${loggedMinutes % 60}m / ${targetMinutes / 60}h Target",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onToggleTimer,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) ZenithColors.CoralEnd else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isTimerRunning) "STOP & LOG ($mm:$ss)" else "START DEEP WORK",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingPillarCard(
    modality: WorkoutModality,
    isCompleted: Boolean,
    onSwitchModality: (WorkoutModality) -> Unit,
    onToggleComplete: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PILLAR III • PHYSICAL CONDITIONING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = modality.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FilterChip(
                    selected = isCompleted,
                    onClick = { onToggleComplete(!isCompleted) },
                    label = {
                        Text(
                            if (isCompleted) "COMPLETED" else "MARK DONE",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutModality.entries.forEach { item ->
                    val selected = item == modality
                    AssistChip(
                        onClick = { onSwitchModality(item) },
                        label = {
                            Text(
                                text = if (item == WorkoutModality.GYM) "GYM ROUTINE" else "HOME CALISTHENICS",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleBlockItemCard(
    block: ScheduleBlockEntity,
    onToggle: () -> Unit,
    onRollOverTomorrow: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (block.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = block.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatMinutesToTime(block.startTimeMinutes)} • ${block.durationMinutes}M",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        block.examSubjectTag?.let { subject ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (block.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            if (!block.isCompleted) {
                IconButton(onClick = onRollOverTomorrow) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Roll over to tomorrow",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBlueprintPrompt(onOpenSheet: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSheet() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text("NO TIME BLOCKS LOCKED", style = MaterialTheme.typography.labelSmall)
                Text(
                    "Tap to launch the Night-Before Blueprint Editor and lock in your exam subjects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatMinutesToTime(totalMinutes: Int): String {
    val hrs = (totalMinutes / 60) % 24
    val mins = totalMinutes % 60
    val period = if (hrs < 12) "AM" else "PM"
    val displayHr = when {
        hrs == 0 -> 12
        hrs > 12 -> hrs - 12
        else -> hrs
    }
    return "%02d:%02d %s".format(displayHr, mins, period)
}
