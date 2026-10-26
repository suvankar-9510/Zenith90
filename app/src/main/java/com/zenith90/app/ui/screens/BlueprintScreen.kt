package com.zenith90.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.BlockCategory
import com.zenith90.app.data.local.ScheduleBlockEntity
import com.zenith90.app.data.local.TaskPriority
import com.zenith90.app.ui.components.HapticTimeSlider
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BlueprintScreen(
    state: ZenithUiState,
    onToggleBlockCompletion: (ScheduleBlockEntity) -> Unit,
    onRescheduleBlockTomorrow: (ScheduleBlockEntity) -> Unit,
    onDeleteScheduleBlock: (ScheduleBlockEntity) -> Unit,
    onSaveBlock: (targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: BlockCategory, priority: TaskPriority) -> Unit
) {
    val view = LocalView.current
    var selectedDateOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow, 2 = Day after...
    var showCreateDialog by remember { mutableStateOf(false) }

    val todayDate = LocalDate.now()
    val selectedDate = todayDate.plusDays(selectedDateOffset.toLong())
    val selectedEpochDay = selectedDate.toEpochDay()

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val monthTitle = selectedDate.format(monthFormatter)

    val todayBlocks = state.currentDaySnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()
    val tomorrowBlocks = state.tomorrowSnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()

    // Determine blocks for currently selected date
    val activeBlocks = when (selectedDateOffset) {
        0 -> todayBlocks
        1 -> tomorrowBlocks
        else -> emptyList()
    }

    val totalMinutes = activeBlocks.sumOf { it.durationMinutes }
    val completedCount = activeBlocks.count { it.isCompleted }

    // 7-day strip centered on today
    val weekDays = (-2..4).map { todayDate.plusDays(it.toLong()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenithColors.BotanicalBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        // --- 1. TOP BAR (MONTH PILL DROPDOWN & BACK BUTTON) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ZenithColors.BotanicalSurface,
                    border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            PixelHaptics.crispTick(view)
                            if (selectedDateOffset > 0) selectedDateOffset--
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = ZenithColors.TextHeading,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Month Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ZenithColors.BotanicalSurface,
                    border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZenithColors.TextHeading
                        )
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = ZenithColors.ForestGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 2. BOLD HEADLINE (Matches Right Screen: "Your Daily Planner, Calendar.") ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Your Daily",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Normal,
                    color = ZenithColors.TextHeading
                )
                Text(
                    text = "Planner, Calendar.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZenithColors.TextHeading
                )
            }
        }

        // --- 3. HORIZONTAL DATE STRIP (Matches Right Screen) ---
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items(weekDays) { date ->
                    val offset = (date.toEpochDay() - todayDate.toEpochDay()).toInt()
                    val isSelected = offset == selectedDateOffset
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    val dayNum = date.dayOfMonth.toString()

                    Surface(
                        modifier = Modifier
                            .width(46.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                PixelHaptics.crispTick(view)
                                selectedDateOffset = offset.coerceIn(0, 1) // 0=Today, 1=Tomorrow
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isSelected) ZenithColors.ForestGreenPrimary else ZenithColors.BotanicalSurface,
                        border = if (isSelected) null else BorderStroke(1.dp, ZenithColors.BotanicalBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else ZenithColors.TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ZenithColors.TextHeading
                            )
                        }
                    }
                }
            }
        }

        // --- 4. SECTION HEADER (Day Name + Task Count Pill) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZenithColors.TextHeading
                )
                Text(
                    text = "${activeBlocks.size} Tasks (${totalMinutes}m)",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenithColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- 5. VERTICAL TIMELINE WITH PASTEL TASK CARDS (Matches Right Screen) ---
        if (activeBlocks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
                    border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = ZenithColors.MintAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Schedule Blocks Locked In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZenithColors.TextHeading
                        )
                        Text(
                            text = "Structure your day in advance to preserve deep cognitive focus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenithColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("ADD TIME BLOCK", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        } else {
            items(activeBlocks, key = { it.id }) { block ->
                PastelTimelineCard(
                    block = block,
                    isTomorrow = selectedDateOffset == 1,
                    onToggleCompletion = {
                        PixelHaptics.confirmCheck(view)
                        onToggleBlockCompletion(block)
                    },
                    onRolloverTomorrow = {
                        PixelHaptics.crispTick(view)
                        onRescheduleBlockTomorrow(block)
                    },
                    onDeleteBlock = {
                        PixelHaptics.crispTick(view)
                        onDeleteScheduleBlock(block)
                    }
                )
            }
        }

        // --- 6. FLOATING ADD BLOCK ACTION ---
        item {
            Button(
                onClick = {
                    PixelHaptics.crispTick(view)
                    showCreateDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ADD NEW TIME BLOCK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // --- CREATE BLOCK DIALOG ---
    if (showCreateDialog) {
        CreateBlockHapticDialog(
            defaultEpochDay = selectedEpochDay,
            onDismiss = { showCreateDialog = false },
            onConfirm = { targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority ->
                PixelHaptics.confirmCheck(view)
                onSaveBlock(targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PastelTimelineCard(
    block: ScheduleBlockEntity,
    isTomorrow: Boolean,
    onToggleCompletion: () -> Unit,
    onRolloverTomorrow: () -> Unit,
    onDeleteBlock: () -> Unit
) {
    val startTimeStr = formatMinutesToTime(block.startTimeMinutes)
    val endTimeStr = formatMinutesToTime(block.startTimeMinutes + block.durationMinutes)

    // Match the pastel colors from the user's reference UI right screen:
    // Card 1: Soft Pink / Rose (#FFE8EC)
    // Card 2: Soft Butter Yellow (#FEF6D8)
    // Card 3: Soft Neutral Grey (#F4F4F4)
    // Card 4: Soft Sky Cyan (#DDF4FA)
    val (cardBg, cardText, cardAccent) = when (block.category) {
        BlockCategory.STUDY -> if (block.priority == TaskPriority.HIGH) {
            Triple(ZenithColors.PastelRose, ZenithColors.PastelRoseText, ZenithColors.PastelRoseAccent)
        } else {
            Triple(ZenithColors.PastelAmber, ZenithColors.PastelAmberText, ZenithColors.PastelAmberAccent)
        }
        BlockCategory.WORKOUT -> Triple(ZenithColors.PastelLilac, ZenithColors.PastelLilacText, ZenithColors.PastelLilacAccent)
        BlockCategory.ROUTINE -> Triple(ZenithColors.PastelSky, ZenithColors.PastelSkyText, ZenithColors.PastelSkyAccent)
        else -> Triple(ZenithColors.PastelMint, ZenithColors.PastelMintText, ZenithColors.PastelMintAccent)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Time Marker (e.g. 08:00 AM)
        Column(
            modifier = Modifier.width(64.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = startTimeStr.substringBefore(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ZenithColors.TextHeading
            )
            Text(
                text = startTimeStr.substringAfter(" "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = ZenithColors.TextMuted
            )
        }

        // Right Column: Pastel Task Pill Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Avatar Circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (block.category) {
                            BlockCategory.STUDY -> Icons.Outlined.AutoStories
                            BlockCategory.WORKOUT -> Icons.Outlined.FitnessCenter
                            BlockCategory.ROUTINE -> Icons.Outlined.SelfImprovement
                            else -> Icons.Outlined.TaskAlt
                        },
                        contentDescription = null,
                        tint = cardText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (block.isCompleted) TextDecoration.LineThrough else null,
                            color = if (block.isCompleted) cardText.copy(alpha = 0.5f) else ZenithColors.TextHeading
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🕒 $startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardText,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Rollover & Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isTomorrow) {
                        IconButton(onClick = onRolloverTomorrow, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = "Rollover to tomorrow",
                                tint = cardText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Checkbox(
                        checked = block.isCompleted,
                        onCheckedChange = { onToggleCompletion() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ZenithColors.ForestGreenPrimary,
                            uncheckedColor = cardText.copy(alpha = 0.6f)
                        )
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
    onConfirm: (targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: BlockCategory, priority: TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Core") }
    var category by remember { mutableStateOf(BlockCategory.STUDY) }
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
                        .background(ZenithColors.BotanicalBg)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { targetDaySelection = 0 },
                        color = if (targetDaySelection == 0) ZenithColors.ForestGreenPrimary else Color.Transparent
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 0) Color.White else ZenithColors.TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { targetDaySelection = 1 },
                        color = if (targetDaySelection == 1) ZenithColors.ForestGreenPrimary else Color.Transparent
                    ) {
                        Text(
                            text = "TOMORROW (NIGHT PLAN)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 1) Color.White else ZenithColors.TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description / Goal") },
                    placeholder = { Text("e.g. Electrodynamics Chapter 4 Problems") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BlockCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HapticTimeSlider(
                    value = startTimeMinutes,
                    onValueChange = { startTimeMinutes = it },
                    valueRange = 240f..1380f,
                    stepMinutes = 15,
                    title = "Start Time",
                    formattedValue = formatMinutesToTime(startTimeMinutes.toInt()),
                    icon = Icons.Outlined.Schedule
                )

                HapticTimeSlider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 15f..240f,
                    stepMinutes = 15,
                    title = "Block Duration",
                    formattedValue = "${durationMinutes.toInt()} Minutes (${"%.1f".format(durationMinutes / 60f)} hrs)",
                    icon = Icons.Outlined.Timer
                )
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
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
            ) {
                Text("LOCK IN BLOCK", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}
