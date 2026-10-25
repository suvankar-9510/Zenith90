package com.zenith90.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.BlockCategory
import com.zenith90.app.data.local.TaskPriority
import com.zenith90.app.ui.components.HapticTimeSlider
import com.zenith90.app.ui.theme.PixelHaptics
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerBottomSheet(
    onDismiss: () -> Unit,
    onSaveBlock: (
        targetEpochDay: Long,
        startTimeMinutes: Int,
        durationMinutes: Int,
        title: String,
        examSubjectTag: String?,
        category: BlockCategory,
        priority: TaskPriority
    ) -> Unit
) {
    val view = LocalView.current
    var planForTomorrow by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var subjectTag by remember { mutableStateOf("QUANTUM MECH") }
    var startHour by remember { mutableStateOf("08") }
    var startMinute by remember { mutableStateOf("30") }
    var durationMins by remember { mutableStateOf("90") }
    var selectedCategory by remember { mutableStateOf(BlockCategory.STUDY) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH) }

    val quickTags = listOf(
        "QUANTUM MECH", "STAT PHYS", "ELECTRODYNAMICS",
        "SOLID STATE", "NUCLEAR PHYS", "CSIR-NET MOCK", "REVISION"
    )

    data class TimePreset(val label: String, val hour: String, val minute: String, val duration: String)
    val timePresets = listOf(
        TimePreset("06:00 (2h) Morning", "06", "00", "120"),
        TimePreset("08:30 (2.5h) Core", "08", "30", "150"),
        TimePreset("14:00 (2h) Afternoon", "14", "00", "120"),
        TimePreset("17:30 (1.5h) Evening", "17", "30", "90"),
        TimePreset("21:00 (1h) Night Review", "21", "00", "60")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NIGHT-BEFORE BLUEPRINT ARCHITECT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lock In Time-Blocked Objective",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Target Day Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = planForTomorrow,
                    onClick = {
                        PixelHaptics.crispTick(view)
                        planForTomorrow = true
                    },
                    label = { Text("TOMORROW'S BLUEPRINT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = !planForTomorrow,
                    onClick = {
                        PixelHaptics.crispTick(view)
                        planForTomorrow = false
                    },
                    label = { Text("TODAY'S SCHEDULE", style = MaterialTheme.typography.labelSmall) }
                )
            }

            // Quick Presets
            Text(
                text = "QUICK TIME BLOCK PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timePresets.forEach { preset ->
                    AssistChip(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            startHour = preset.hour
                            startMinute = preset.minute
                            durationMins = preset.duration
                        },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // Subject Tag Quick Chips
            Text(
                text = "SUBJECT TAGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTags.forEach { tag ->
                    val isSelected = subjectTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            PixelHaptics.crispTick(view)
                            subjectTag = tag
                        },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Objective / Task Title") },
                placeholder = { Text("e.g. Perturbation Theory Problem Set") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // START TIME SLIDER (WITH HAPTICS)
            val currentStartMinutes = ((startHour.toIntOrNull() ?: 8) * 60 + (startMinute.toIntOrNull() ?: 30)).toFloat()
            HapticTimeSlider(
                value = currentStartMinutes,
                onValueChange = { totalMins ->
                    val h = totalMins.toInt() / 60
                    val m = totalMins.toInt() % 60
                    startHour = "%02d".format(h)
                    startMinute = "%02d".format(m)
                },
                valueRange = 240f..1380f,
                stepMinutes = 15,
                title = "Start Time (Haptic Dial)",
                formattedValue = formatMinutesToTime(currentStartMinutes.toInt()),
                icon = Icons.Outlined.AccessTime
            )

            // DURATION SLIDER (WITH HAPTICS)
            val currentDurationMinutes = (durationMins.toFloatOrNull() ?: 90f).coerceIn(15f, 240f)
            HapticTimeSlider(
                value = currentDurationMinutes,
                onValueChange = { dur ->
                    durationMins = dur.toInt().toString()
                },
                valueRange = 15f..240f,
                stepMinutes = 15,
                title = "Block Duration (Haptic Dial)",
                formattedValue = "${(durationMins.toIntOrNull() ?: 90) / 60}h ${(durationMins.toIntOrNull() ?: 90) % 60}m",
                icon = Icons.Outlined.Timer
            )

            // Priority Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = {
                            PixelHaptics.crispTick(view)
                            selectedPriority = priority
                        },
                        label = { Text(priority.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Lock In Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        PixelHaptics.heavyUnlock(view)
                        val targetEpoch = if (planForTomorrow) {
                            LocalDate.now().plusDays(1).toEpochDay()
                        } else {
                            LocalDate.now().toEpochDay()
                        }
                        val hr = (startHour.toIntOrNull() ?: 8).coerceIn(0, 23)
                        val min = (startMinute.toIntOrNull() ?: 30).coerceIn(0, 59)
                        val dur = (durationMins.toIntOrNull() ?: 60).coerceAtLeast(10)

                        onSaveBlock(
                            targetEpoch,
                            hr * 60 + min,
                            dur,
                            title.trim(),
                            subjectTag.takeIf { it.isNotBlank() },
                            selectedCategory,
                            selectedPriority
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (planForTomorrow) "LOCK IN FOR TOMORROW'S BLUEPRINT" else "LOCK IN FOR TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
