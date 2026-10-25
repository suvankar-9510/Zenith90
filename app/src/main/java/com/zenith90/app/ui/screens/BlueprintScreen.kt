package com.zenith90.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.ScheduleBlockEntity
import com.zenith90.app.data.local.TaskPriority
import com.zenith90.app.ui.components.HapticTimeSlider
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BlueprintScreen(
    state: ZenithUiState,
    onToggleBlockCompletion: (ScheduleBlockEntity) -> Unit,
    onRescheduleBlockTomorrow: (ScheduleBlockEntity) -> Unit,
    onDeleteScheduleBlock: (ScheduleBlockEntity) -> Unit,
    onSaveBlock: (targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: String, priority: TaskPriority) -> Unit
) {
    val view = LocalView.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow
    var showCreateDialog by remember { mutableStateOf(false) }

    val todayDate = LocalDate.now()
    val tomorrowDate = todayDate.plusDays(1)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM")

    val todayBlocks = state.currentDaySnapshot?.scheduleBlocks ?: emptyList()
    val tomorrowBlocks = state.tomorrowScheduleBlocks

    val activeBlocks = if (selectedTab == 0) todayBlocks else tomorrowBlocks
    val activeDate = if (selectedTab == 0) todayDate else tomorrowDate
    val activeEpochDay = activeDate.toEpochDay()

    val completedCount = activeBlocks.count { it.isCompleted }
    val totalCount = activeBlocks.size
    val totalMinutes = activeBlocks.sumOf { it.durationMinutes }
    val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        // --- 1. SCREEN HEADER (MATERIAL YOU EXPRESSIVE TITLE) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "TACTICAL EXECUTION ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TIME-BLOCKED BLUEPRINT",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Precision schedule architecture for high cognitive output",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 2. TODAY VS TOMORROW SEGMENTED TABS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Today Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            PixelHaptics.crispTick(view)
                            selectedTab = 0
                        },
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${todayDate.format(dateFormatter)} • $completedCount/$totalCount Done",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tomorrow Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            PixelHaptics.crispTick(view)
                            selectedTab = 1
                        },
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOMORROW (NIGHT PLAN)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${tomorrowDate.format(dateFormatter)} • ${tomorrowBlocks.size} Planned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 3. SCHEDULE SUMMARY CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedTab == 0) "TODAY'S EXECUTION TRAJECTORY" else "TOMORROW'S PRE-LOCKED ARCHITECTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${totalMinutes / 60}h ${totalMinutes % 60}m Total Planned (${activeBlocks.size} blocks)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD BLOCK", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (selectedTab == 0 && totalCount > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Completion Velocity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progressRatio * 100).toInt()}% Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 4. QUICK TIME PRESETS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QUICK PRESET TIME BLOCKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SuggestionChip(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                onSaveBlock(activeEpochDay, 360, 150, "Morning Deep Work Session", "CORE", "STUDY", TaskPriority.HIGH)
                            },
                            label = { Text("+ 06:00 AM Morning Core (2.5h)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                onSaveBlock(activeEpochDay, 600, 150, "Midday Problem Grind", "PRACTICE", "STUDY", TaskPriority.HIGH)
                            },
                            label = { Text("+ 10:00 AM Midday Grind (2.5h)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                onSaveBlock(activeEpochDay, 990, 60, "Physical Conditioning Session", "WORKOUT", "TRAINING", TaskPriority.MEDIUM)
                            },
                            label = { Text("+ 04:30 PM Workout (1h)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                onSaveBlock(activeEpochDay, 1110, 120, "Evening Revision & Mock Review", "REVISION", "STUDY", TaskPriority.HIGH)
                            },
                            label = { Text("+ 06:30 PM Revision (2h)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                onSaveBlock(activeEpochDay, 1290, 30, "Night Protocol: Reflection & Tomorrow Plan", "NIGHT", "PLANNING", TaskPriority.MEDIUM)
                            },
                            label = { Text("+ 09:30 PM Night Protocol (30m)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // --- 5. TIMELINE SCHEDULE CARDS ---
        item {
            Text(
                text = if (selectedTab == 0) "TODAY'S CHRONOLOGICAL TIMELINE" else "TOMORROW'S PRE-PLANNED SEQUENCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activeBlocks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (selectedTab == 0) "No time blocks scheduled for today" else "Tomorrow's blueprint is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedTab == 0) "Lock in your deep study and training hours to eliminate drift." else "Lock in tomorrow's schedule tonight so you wake up with absolute clarity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CREATE FIRST BLOCK", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            items(activeBlocks, key = { it.id }) { block ->
                BlueprintTaskCard(
                    block = block,
                    onToggleCompletion = {
                        PixelHaptics.confirmCheck(view)
                        onToggleBlockCompletion(block)
                    },
                    onRolloverTomorrow = {
                        PixelHaptics.crispTick(view)
                        onRescheduleBlockTomorrow(block)
                    },
                    onDelete = {
                        PixelHaptics.crispTick(view)
                        onDeleteScheduleBlock(block)
                    }
                )
            }
        }
    }

    // --- CREATE BLOCK DIALOG (FEATURING HAPTIC SLIDERS) ---
    if (showCreateDialog) {
        CreateBlockHapticDialog(
            defaultEpochDay = activeEpochDay,
            onDismiss = { showCreateDialog = false },
            onConfirm = { targetEpoch, startMins, durMins, title, tag, cat, prio ->
                showCreateDialog = false
                onSaveBlock(targetEpoch, startMins, durMins, title, tag, cat, prio)
            }
        )
    }
}

@Composable
private fun BlueprintTaskCard(
    block: ScheduleBlockEntity,
    onToggleCompletion: () -> Unit,
    onRolloverTomorrow: () -> Unit,
    onDelete: () -> Unit
) {
    val startTimeStr = formatMinutesToTime(block.startTimeMinutes)
    val endTimeStr = formatMinutesToTime(block.startTimeMinutes + block.durationMinutes)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (block.isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completion Checkbox
            Checkbox(
                checked = block.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = block.examSubjectTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }
                    if (block.priority == TaskPriority.HIGH) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ZenithColors.CoralEnd.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "HIGH",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZenithColors.CoralEnd,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Text(
                    text = block.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (block.isCompleted) TextDecoration.LineThrough else null,
                        color = if (block.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick Actions: Rollover & Delete
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onRolloverTomorrow,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = "Rollover to tomorrow",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete block",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBlockHapticDialog(
    defaultEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: String, priority: TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Core") }
    var category by remember { mutableStateOf("STUDY") }
    var priority by remember { mutableStateOf(TaskPriority.HIGH) }
    var startTimeMinutes by remember { mutableFloatStateOf(360f) } // 06:00 AM default
    var durationMinutes by remember { mutableFloatStateOf(90f) }   // 1.5 hours default
    var targetDaySelection by remember { mutableIntStateOf(if (defaultEpochDay == LocalDate.now().toEpochDay()) 0 else 1) }

    val todayEpoch = LocalDate.now().toEpochDay()
    val tomorrowEpoch = todayEpoch + 1
    val chosenEpoch = if (targetDaySelection == 0) todayEpoch else tomorrowEpoch

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ARCHITECT TIME BLOCK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Day Target Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { targetDaySelection = 0 },
                        color = if (targetDaySelection == 0) MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { targetDaySelection = 1 },
                        color = if (targetDaySelection == 1) MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Text(
                            text = "TOMORROW (NIGHT PLAN)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description / Goal") },
                    placeholder = { Text("e.g. Electrodynamics Chapter 4 Problems") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // START TIME SLIDER (WITH HAPTICS)
                HapticTimeSlider(
                    value = startTimeMinutes,
                    onValueChange = { startTimeMinutes = it },
                    valueRange = 240f..1380f, // 04:00 AM to 11:00 PM
                    stepMinutes = 15,
                    title = "Start Time (Haptic Dial)",
                    formattedValue = formatMinutesToTime(startTimeMinutes.toInt()),
                    icon = Icons.Outlined.Schedule
                )

                // DURATION SLIDER (WITH HAPTICS)
                HapticTimeSlider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 15f..240f, // 15 mins to 4 hours
                    stepMinutes = 15,
                    title = "Block Duration (Haptic Dial)",
                    formattedValue = "${durationMinutes.toInt() / 60}h ${durationMinutes.toInt() % 60}m",
                    icon = Icons.Outlined.Timer
                )

                // Subject Tag Suggestions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Subject / Category Tag:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Physics", "Math", "Calisthenics", "Revision").forEach { s ->
                            FilterChip(
                                selected = tag.equals(s, ignoreCase = true),
                                onClick = { tag = s },
                                label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            chosenEpoch,
                            startTimeMinutes.toInt(),
                            durationMinutes.toInt(),
                            title,
                            tag,
                            category,
                            priority
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("SAVE TIME BLOCK", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}

private fun formatMinutesToTime(minutes: Int): String {
    val hrs = (minutes / 60) % 24
    val mins = minutes % 60
    val period = if (hrs >= 12) "PM" else "AM"
    val displayHrs = when {
        hrs == 0 -> 12
        hrs > 12 -> hrs - 12
        else -> hrs
    }
    return "%02d:%02d %s".format(displayHrs, mins, period)
}
