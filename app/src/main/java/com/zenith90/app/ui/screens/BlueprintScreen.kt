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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.BlockCategory
import com.zenith90.app.data.local.ScheduleBlockEntity
import com.zenith90.app.data.local.TaskPriority
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
    onSaveBlock: (id: Long, targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: BlockCategory, priority: TaskPriority) -> Unit
) {
    val view = LocalView.current
    var selectedDateOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ScheduleBlockEntity?>(null) }

    val todayDate = LocalDate.now()
    val selectedDate = todayDate.plusDays(selectedDateOffset.toLong())
    val selectedEpochDay = selectedDate.toEpochDay()

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val monthTitle = selectedDate.format(monthFormatter)

    val todayBlocks = state.currentDaySnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()
    val tomorrowBlocks = state.tomorrowSnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()

    val activeBlocks = when (selectedDateOffset) {
        0 -> todayBlocks
        1 -> tomorrowBlocks
        else -> emptyList()
    }

    val totalMinutes = activeBlocks.sumOf { it.durationMinutes }
    val weekDays = (-2..4).map { todayDate.plusDays(it.toLong()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        // --- 1. TOP BAR (MONTH PILL DROPDOWN & DAY NAVIGATION) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Month Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 2. BOLD HEADLINE ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Your Daily",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Planner, Calendar.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // --- 3. HORIZONTAL DATE STRIP ---
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
                                selectedDateOffset = offset.coerceIn(0, 1)
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- 4. SECTION HEADER ---
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeBlocks.size} Tasks (${totalMinutes}m)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- 5. VERTICAL TIMELINE WITH PASTEL TASK CARDS ---
        if (activeBlocks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Schedule Blocks Locked In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Structure your day in advance to preserve deep cognitive focus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                PixelHaptics.crispTick(view)
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("ADD TIME BLOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
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
                    onEditBlock = {
                        PixelHaptics.crispTick(view)
                        editingBlock = block
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("ADD NEW TIME BLOCK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    // --- CREATE OR EDIT BLOCK COMPACT DIALOG ---
    if (showCreateDialog) {
        CompactBlockEditorDialog(
            initialBlock = null,
            defaultEpochDay = selectedEpochDay,
            onDismiss = { showCreateDialog = false },
            onConfirm = { id, targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority ->
                PixelHaptics.confirmCheck(view)
                onSaveBlock(id, targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority)
                showCreateDialog = false
            }
        )
    }

    if (editingBlock != null) {
        val block = editingBlock!!
        CompactBlockEditorDialog(
            initialBlock = block,
            defaultEpochDay = block.dateEpochDay,
            onDismiss = { editingBlock = null },
            onConfirm = { id, targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority ->
                PixelHaptics.confirmCheck(view)
                onSaveBlock(id, targetEpochDay, startTimeMinutes, durationMinutes, title, tag, category, priority)
                editingBlock = null
            },
            onDelete = {
                PixelHaptics.crispTick(view)
                onDeleteScheduleBlock(block)
                editingBlock = null
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
    onEditBlock: () -> Unit,
    onDeleteBlock: () -> Unit
) {
    val startTimeStr = formatMinutesToTime(block.startTimeMinutes)
    val endTimeStr = formatMinutesToTime(block.startTimeMinutes + block.durationMinutes)

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
        // Left Column: Time Marker
        Column(
            modifier = Modifier.width(64.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = startTimeStr.substringBefore(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = startTimeStr.substringAfter(" "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditBlock() },
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (block.isCompleted) TextDecoration.LineThrough else null,
                            color = if (block.isCompleted) cardText.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
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

                // Edit, Rollover & Checkbox Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditBlock, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Block",
                            tint = cardText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = cardText.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

// --- STREAMLINED & COMPACT TIME BLOCK EDITOR DIALOG ---
@Composable
fun CompactBlockEditorDialog(
    initialBlock: ScheduleBlockEntity? = null,
    defaultEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (id: Long, targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: BlockCategory, priority: TaskPriority) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = initialBlock != null
    var title by remember { mutableStateOf(initialBlock?.title ?: "") }
    var tag by remember { mutableStateOf(initialBlock?.examSubjectTag ?: "Core") }
    var category by remember { mutableStateOf(initialBlock?.category ?: BlockCategory.STUDY) }
    var priority by remember { mutableStateOf(initialBlock?.priority ?: TaskPriority.HIGH) }
    var startTimeMinutes by remember { mutableIntStateOf(initialBlock?.startTimeMinutes ?: 360) }
    var durationMinutes by remember { mutableIntStateOf(initialBlock?.durationMinutes ?: 90) }
    var targetDaySelection by remember {
        mutableIntStateOf(if (defaultEpochDay == LocalDate.now().toEpochDay()) 0 else 1)
    }

    val todayEpoch = LocalDate.now().toEpochDay()
    val tomorrowEpoch = todayEpoch + 1
    val chosenEpoch = if (targetDaySelection == 0) todayEpoch else tomorrowEpoch

    val quickPresets = listOf(
        Pair("06:00 AM", 360),
        Pair("08:30 AM", 510),
        Pair("10:00 AM", 600),
        Pair("02:00 PM", 840),
        Pair("05:00 PM", 1020),
        Pair("08:00 PM", 1200)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "EDIT TIME BLOCK" else "NEW TIME BLOCK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day Target Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { targetDaySelection = 0 },
                        color = if (targetDaySelection == 0) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { targetDaySelection = 1 },
                        color = if (targetDaySelection == 1) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = "TOMORROW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetDaySelection == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Compact Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Goal / Objective") },
                    placeholder = { Text("e.g. Electrodynamics Problems") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Compact Category Chips
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

                // Compact Start Time Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start: ${formatMinutesToTime(startTimeMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { startTimeMinutes = (startTimeMinutes - 15).coerceIn(0, 1425) },
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("-15m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                            FilledTonalButton(
                                onClick = { startTimeMinutes = (startTimeMinutes + 15).coerceIn(0, 1425) },
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+15m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }

                    // Quick Time Chips Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(quickPresets) { preset ->
                            SuggestionChip(
                                onClick = { startTimeMinutes = preset.second },
                                label = { Text(preset.first, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Compact Duration Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration: ${durationMinutes}m (${"%.1f".format(durationMinutes / 60f)} hrs)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { durationMinutes = (durationMinutes - 15).coerceAtLeast(15) },
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("-15m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                            FilledTonalButton(
                                onClick = { durationMinutes = (durationMinutes + 15).coerceAtMost(360) },
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+15m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(15, 30, 45, 60, 90, 120, 150).forEach { dur ->
                            SuggestionChip(
                                onClick = { durationMinutes = dur },
                                label = { Text("${dur}m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) }
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
                            initialBlock?.id ?: 0L,
                            chosenEpoch,
                            startTimeMinutes,
                            durationMinutes,
                            title,
                            tag,
                            category,
                            priority
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEditing) "SAVE CHANGES" else "LOCK IN BLOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("DELETE", style = MaterialTheme.typography.labelSmall, color = ZenithColors.PastelRoseText)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )
}
