package com.zenith90.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.BlockCategory
import com.zenith90.app.data.local.TaskPriority
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NIGHT-BEFORE BLUEPRINT ARCHITECT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Lock In Time-Blocked Objective",
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = planForTomorrow,
                    onClick = {
                        PixelHaptics.crispTick(view)
                        planForTomorrow = true
                    },
                    label = { Text("TOMORROW'S BLUEPRINT", style = MaterialTheme.typography.labelSmall) }
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Time Block / Objective Title") },
                placeholder = { Text("e.g., Perturbation Theory Problem Set") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subjectTag,
                    onValueChange = { subjectTag = it },
                    label = { Text("Exam Subject Tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationMins,
                    onValueChange = { durationMins = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (Min)") },
                    modifier = Modifier.width(130.dp),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startHour,
                    onValueChange = { startHour = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Start Hour (00-23)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = startMinute,
                    onValueChange = { startMinute = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Start Min (00-59)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Text(
                text = "EXAM PRIORITY WEIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

            Spacer(Modifier.height(6.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("LOCK IN TIME BLOCK", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
