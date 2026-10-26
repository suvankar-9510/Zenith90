package com.zenith90.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.*
import com.zenith90.app.ui.components.HapticTimeSlider
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatMinutesToTime(totalMinutes: Int): String {
    val hrs = (totalMinutes / 60) % 24
    val mins = totalMinutes % 60
    val amPm = if (hrs < 12) "AM" else "PM"
    val displayHrs = when {
        hrs == 0 -> 12
        hrs > 12 -> hrs - 12
        else -> hrs
    }
    return "%02d:%02d %s".format(displayHrs, mins, amPm)
}

@Composable
fun DashboardScreen(
    state: ZenithUiState,
    onLogWakeUpNow: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onConfirmPomodoro: (minutes: Int, subjectTag: String) -> Unit,
    onDiscardPomodoro: () -> Unit,
    onQuickAddStudyMinutes: (Int) -> Unit,
    onUpdateExactStudyMinutes: ((minutes: Int, targetMinutes: Int?) -> Unit)? = null,
    onSwitchWorkoutModality: (WorkoutModality) -> Unit,
    onToggleWorkoutCompleted: (Boolean) -> Unit,
    onAddWorkoutExercise: (name: String, targetSets: Int, repsAndLoad: String) -> Unit,
    onIncrementWorkoutSet: (WorkoutEntryEntity) -> Unit,
    onDeleteWorkoutSet: (WorkoutEntryEntity) -> Unit,
    onToggleBlockCompletion: (ScheduleBlockEntity) -> Unit,
    onRescheduleBlockTomorrow: (ScheduleBlockEntity) -> Unit,
    onDeleteScheduleBlock: (ScheduleBlockEntity) -> Unit,
    onSaveBlock: ((id: Long, targetEpochDay: Long, startTimeMinutes: Int, durationMinutes: Int, title: String, tag: String, category: BlockCategory, priority: TaskPriority) -> Unit)? = null,
    onOpenNightBlueprintSheet: () -> Unit,
    onUpdateUserSettings: (examTitle: String, targetDate: LocalDate, wakeMinutes: Int, studyMinutes: Int) -> Unit,
    onUpdateTodayTargets: (wakeMinutes: Int, studyMinutes: Int) -> Unit,
    onTriggerCloudSync: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToBlueprint: (() -> Unit)? = null,
    onSendTestNotification: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val dayLog = state.currentDaySnapshot?.dailyLog ?: DailyLogEntity(
        dateEpochDay = state.todayDate.toEpochDay(),
        targetWakeTimeMinutes = state.userSettings.targetWakeMinutes,
        targetStudyMinutes = state.userSettings.targetStudyMinutes
    )
    val todayBlocks = state.currentDaySnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()
    val tomorrowBlocks = state.tomorrowSnapshot?.scheduleBlocks?.sortedBy { it.startTimeMinutes } ?: emptyList()
    val workoutEntries = state.currentDaySnapshot?.workoutEntries ?: emptyList()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showExamConfigDialog by remember { mutableStateOf(false) }
    var showTargetEditDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showEditStudyDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ScheduleBlockEntity?>(null) }
    var pendingBoostMinutes by remember { mutableStateOf<Int?>(null) }
    var selectedBlueprintTab by remember { mutableStateOf(0) } // 0 = Today, 1 = Tomorrow

    // Automatic first-time onboarding prompt if user hasn't set their custom exam
    LaunchedEffect(state.userSettings.hasCompletedOnboarding) {
        if (!state.userSettings.hasCompletedOnboarding && state.userProfile != null) {
            showExamConfigDialog = true
        }
    }

    // --- DIALOG 1: POMODORO VALIDATION CONFIRMATION ---
    if (state.pendingPomodoroMinutes != null) {
        var selectedSubject by remember { mutableStateOf("Core Study") }
        AlertDialog(
            onDismissRequest = onDiscardPomodoro,
            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "VALIDATE DEEP FOCUS SESSION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pomodoro session ended with ${state.pendingPomodoroMinutes} elapsed minutes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Confirm study topic before crediting time to avoid logging idle hours:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PHYSICS", "MATH", "CHEMISTRY", "REVISION").forEach { tag ->
                            FilterChip(
                                selected = selectedSubject == tag,
                                onClick = {
                                    PixelHaptics.crispTick(view)
                                    selectedSubject = tag
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PixelHaptics.confirmCheck(view)
                        onConfirmPomodoro(state.pendingPomodoroMinutes, selectedSubject)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CONFIRM & LOG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    PixelHaptics.crispTick(view)
                    onDiscardPomodoro()
                }) {
                    Text("DISCARD (WAS IDLE)", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG 1.5: VERIFY PILLAR II STUDY BOOST (+25M, +50M) ---
    if (pendingBoostMinutes != null) {
        val boostMins = pendingBoostMinutes!!
        var selectedSubject by remember { mutableStateOf("Core Concepts") }
        var focusIntensity by remember { mutableStateOf("HIGH FLOW 🔥") }

        AlertDialog(
            onDismissRequest = { pendingBoostMinutes = null },
            icon = { Icon(Icons.Outlined.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "VERIFY PILLAR II DEEP BOOST (+$boostMins M)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Confirm you completed this +$boostMins minute deep study session to verify trajectory score & streaks:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Select Focus Subject:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Physics", "Math", "Chemistry", "Revision", "Problems").forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = {
                                    PixelHaptics.crispTick(view)
                                    selectedSubject = subj
                                },
                                label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HIGH FLOW 🔥", "STANDARD 📖").forEach { intensity ->
                            FilterChip(
                                selected = focusIntensity == intensity,
                                onClick = {
                                    PixelHaptics.crispTick(view)
                                    focusIntensity = intensity
                                },
                                label = { Text(intensity, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PixelHaptics.confirmCheck(view)
                        onConfirmPomodoro(boostMins, "$selectedSubject • $focusIntensity")
                        pendingBoostMinutes = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("VERIFY & CREDIT +${boostMins}M", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBoostMinutes = null }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG 1.8: COMPACT EDIT STUDY SESSION & TARGETS ---
    if (showEditStudyDialog) {
        CompactStudyEditorDialog(
            currentLoggedMinutes = dayLog.totalStudyMinutes,
            currentTargetMinutes = dayLog.targetStudyMinutes,
            onDismiss = { showEditStudyDialog = false },
            onConfirm = { newLoggedMins, newTargetMins ->
                PixelHaptics.confirmCheck(view)
                if (onUpdateExactStudyMinutes != null) {
                    onUpdateExactStudyMinutes(newLoggedMins, newTargetMins)
                } else {
                    val diff = newLoggedMins - dayLog.totalStudyMinutes
                    if (diff != 0) onQuickAddStudyMinutes(diff)
                    if (newTargetMins != dayLog.targetStudyMinutes) onUpdateTodayTargets(dayLog.targetWakeTimeMinutes, newTargetMins)
                }
                showEditStudyDialog = false
            }
        )
    }

    // --- DIALOG: COMPACT EDIT SCHEDULE BLOCK (FOR TODAY/TOMORROW CARDS) ---
    if (editingBlock != null) {
        val block = editingBlock!!
        CompactBlockEditorDialog(
            initialBlock = block,
            defaultEpochDay = block.dateEpochDay,
            onDismiss = { editingBlock = null },
            onConfirm = { id, epochDay, startMins, durMins, title, tag, category, priority ->
                PixelHaptics.confirmCheck(view)
                onSaveBlock?.invoke(id, epochDay, startMins, durMins, title, tag, category, priority)
                editingBlock = null
            },
            onDelete = {
                PixelHaptics.crispTick(view)
                onDeleteScheduleBlock(block)
                editingBlock = null
            }
        )
    }

    // --- DIALOG 2: ONBOARDING & EXAM CONFIGURATION (WITH HAPTIC SLIDERS) ---
    if (showExamConfigDialog) {
        var examTitleInput by remember { mutableStateOf(state.userSettings.examTitle) }
        var targetDaysInput by remember { mutableStateOf(state.daysRemaining.toString()) }
        var studyHoursInput by remember { mutableFloatStateOf((state.userSettings.targetStudyMinutes / 60).toFloat()) }
        var wakeMinsInput by remember { mutableFloatStateOf(state.userSettings.targetWakeMinutes.toFloat()) }

        AlertDialog(
            onDismissRequest = { showExamConfigDialog = false },
            title = {
                Text(
                    text = "CUSTOMIZE TRANSFORMATION & EXAM TARGET",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = examTitleInput,
                        onValueChange = { examTitleInput = it },
                        label = { Text("Target Exam / Goal Title") },
                        placeholder = { Text("e.g. CSIR-NET Physical Sciences") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = targetDaysInput,
                        onValueChange = { targetDaysInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days Until Target Exam / Deadline") },
                        placeholder = { Text("87") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    HapticTimeSlider(
                        value = wakeMinsInput,
                        onValueChange = { wakeMinsInput = it },
                        valueRange = 240f..540f,
                        stepMinutes = 15,
                        title = "Default Daily Wake Target",
                        formattedValue = formatMinutesToTime(wakeMinsInput.toInt()),
                        icon = Icons.Outlined.WbSunny
                    )

                    HapticTimeSlider(
                        value = studyHoursInput,
                        onValueChange = { studyHoursInput = it },
                        valueRange = 1f..14f,
                        stepMinutes = 1,
                        title = "Default Daily Study Target",
                        formattedValue = "%.1f Hours (%d mins)".format(studyHoursInput, (studyHoursInput * 60).toInt()),
                        icon = Icons.Outlined.AutoStories
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PixelHaptics.heavyUnlock(view)
                        val days = targetDaysInput.toLongOrNull() ?: 87L
                        val targetDate = LocalDate.now().plusDays(days)
                        val studyMins = (studyHoursInput * 60).toInt()
                        val wakeMins = wakeMinsInput.toInt()

                        onUpdateUserSettings(examTitleInput, targetDate, wakeMins, studyMins)
                        showExamConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("LOCK IN MY GOALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExamConfigDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG 3: EDIT TODAY'S TARGETS DIRECTLY (WITH HAPTIC SLIDERS) ---
    if (showTargetEditDialog) {
        var todayStudyHrs by remember { mutableFloatStateOf(dayLog.targetStudyMinutes / 60f) }
        var todayWakeMins by remember { mutableFloatStateOf(dayLog.targetWakeTimeMinutes.toFloat()) }

        AlertDialog(
            onDismissRequest = { showTargetEditDialog = false },
            title = {
                Text(
                    text = "EDIT TODAY'S 3-PILLAR TARGETS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HapticTimeSlider(
                        value = todayWakeMins,
                        onValueChange = { todayWakeMins = it },
                        valueRange = 240f..540f,
                        stepMinutes = 15,
                        title = "Today's Wake-Up Target",
                        formattedValue = formatMinutesToTime(todayWakeMins.toInt()),
                        icon = Icons.Outlined.WbSunny
                    )

                    HapticTimeSlider(
                        value = todayStudyHrs,
                        onValueChange = { todayStudyHrs = it },
                        valueRange = 1f..14f,
                        stepMinutes = 1,
                        title = "Today's Study Hours Target",
                        formattedValue = "%.1f Hours (%d mins)".format(todayStudyHrs, (todayStudyHrs * 60).toInt()),
                        icon = Icons.Outlined.AutoStories
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PixelHaptics.confirmCheck(view)
                        onUpdateTodayTargets(todayWakeMins.toInt(), (todayStudyHrs * 60).toInt())
                        showTargetEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("APPLY TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetEditDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG 4: ADD WORKOUT EXERCISE ---
    if (showAddExerciseDialog) {
        var exerciseName by remember { mutableStateOf("") }
        var targetSets by remember { mutableStateOf("4") }
        var repsLoad by remember { mutableStateOf("8-12 reps") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = {
                Text(
                    text = "ADD CUSTOM EXERCISE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") },
                        placeholder = { Text("e.g. Barbell Squats or Pullups") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = targetSets,
                        onValueChange = { targetSets = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Sets") },
                        placeholder = { Text("4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = repsLoad,
                        onValueChange = { repsLoad = it },
                        label = { Text("Reps & Load / Target") },
                        placeholder = { Text("e.g. 10 reps @ 80kg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exerciseName.isNotBlank()) {
                            PixelHaptics.confirmCheck(view)
                            onAddWorkoutExercise(exerciseName, targetSets.toIntOrNull() ?: 4, repsLoad)
                            showAddExerciseDialog = false
                        }
                    },
                    enabled = exerciseName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- PROFILE & FIREBASE SYNC DIALOG ---
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = state.userProfile?.displayName ?: "Zenith Scholar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = state.userProfile?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: ${state.examTitle} (${state.daysRemaining} days left)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Status: ${state.userProfile?.lastSyncedStatus ?: "Local SQLite Active"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = {
                            showProfileDialog = false
                            showExamConfigDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CONFIGURE EXAM & TARGETS", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PixelHaptics.confirmCheck(view)
                        onTriggerCloudSync()
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SYNC TO FIREBASE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProfileDialog = false
                    onSignOut()
                }) {
                    Text("SIGN OUT", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 100.dp)
    ) {
        // --- 1. TOP GREETING HEADER ---
        item {
            BotanicalTopHeader(
                userName = state.userProfile?.displayName ?: "Scholar",
                onNotificationClick = {
                    PixelHaptics.crispTick(view)
                    onSendTestNotification?.invoke()
                },
                onProfileClick = {
                    PixelHaptics.crispTick(view)
                    showProfileDialog = true
                }
            )
        }

        // --- 2. "CREATE NEW TASK / BLUEPRINT" HERO BANNER ---
        item {
            CreateNewTaskBanner(
                onClick = {
                    PixelHaptics.crispTick(view)
                    if (onNavigateToBlueprint != null) {
                        onNavigateToBlueprint()
                    } else {
                        onOpenNightBlueprintSheet()
                    }
                }
            )
        }

        // --- 3. 2x2 "SUMMARY" GRID (WITH CIRCULAR COLOR FILLING) ---
        item {
            val totalTargetSets = workoutEntries.sumOf { it.targetSets }
            val completedSets = workoutEntries.sumOf { it.completedSets }

            SummaryGridSection(
                targetWakeMinutes = dayLog.targetWakeTimeMinutes,
                actualWakeMinutes = dayLog.actualWakeTimeMinutes,
                isWakeGoalMet = dayLog.isWakeGoalMet,
                totalStudyMinutes = dayLog.totalStudyMinutes,
                targetStudyMinutes = dayLog.targetStudyMinutes,
                workoutCompleted = dayLog.workoutCompleted,
                totalTargetSets = totalTargetSets,
                workoutSetsCount = completedSets,
                equilibriumRatio = dayLog.completionRatio,
                onTuneTargets = {
                    PixelHaptics.crispTick(view)
                    showTargetEditDialog = true
                }
            )
        }

        // --- 4. "ACTIVITY" WEEKLY BAR CHART ---
        item {
            ActivityWeeklyChartCard(
                historyLogs = state.fourMonthHistory,
                todayDate = state.todayDate,
                todayStudyMinutes = dayLog.totalStudyMinutes,
                targetStudyMinutes = dayLog.targetStudyMinutes
            )
        }

        // --- 5. PILLAR II: DEEP FOCUS ENGINE & POMODORO (COMPACT & EDITABLE) ---
        item {
            BotanicalFocusMonitorCard(
                loggedMinutes = dayLog.totalStudyMinutes,
                targetMinutes = dayLog.targetStudyMinutes,
                isTimerRunning = state.isFocusTimerRunning,
                activeSeconds = state.activeFocusSeconds,
                onToggleTimer = {
                    PixelHaptics.timerPulse(context)
                    onToggleFocusTimer()
                },
                onQuickAddMinutes = { mins ->
                    PixelHaptics.crispTick(view)
                    pendingBoostMinutes = mins
                },
                onOpenEdit = {
                    PixelHaptics.crispTick(view)
                    showEditStudyDialog = true
                }
            )
        }

        // --- 6. PILLAR I: CIRCADIAN WAKE ANCHOR CARD ---
        item {
            BotanicalWakeAnchorCard(
                targetMinutes = dayLog.targetWakeTimeMinutes,
                actualMinutes = dayLog.actualWakeTimeMinutes,
                onLogWakeNow = {
                    PixelHaptics.confirmCheck(view)
                    onLogWakeUpNow()
                }
            )
        }

        // --- 7. PILLAR III: PHYSICAL CONDITIONING CARD ---
        item {
            BotanicalTrainingCard(
                modality = dayLog.workoutModality,
                isCompleted = dayLog.workoutCompleted,
                entries = workoutEntries,
                onSwitchModality = onSwitchWorkoutModality,
                onToggleComplete = {
                    PixelHaptics.confirmCheck(view)
                    onToggleWorkoutCompleted(it)
                },
                onOpenAddExercise = {
                    PixelHaptics.crispTick(view)
                    showAddExerciseDialog = true
                },
                onIncrementSet = { entry ->
                    PixelHaptics.confirmCheck(view)
                    onIncrementWorkoutSet(entry)
                },
                onDeleteSet = { entry ->
                    PixelHaptics.crispTick(view)
                    onDeleteWorkoutSet(entry)
                }
            )
        }

        // --- 8. TIME-BLOCKED BLUEPRINT SECTION (TODAY / TOMORROW) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIME-BLOCKED BLUEPRINT ARCHITECT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        PixelHaptics.crispTick(view)
                        onOpenNightBlueprintSheet()
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("+ NIGHT PLANNER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Day Switcher Tabs (Today vs Tomorrow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    val todayActive = selectedBlueprintTab == 0
                    Button(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            selectedBlueprintTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (todayActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (todayActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TODAY (${todayBlocks.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            selectedBlueprintTab = 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!todayActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!todayActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TOMORROW (${tomorrowBlocks.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val activeBlockList = if (selectedBlueprintTab == 0) todayBlocks else tomorrowBlocks

        if (activeBlockList.isEmpty()) {
            item {
                EmptyBlueprintPrompt(
                    isTomorrow = selectedBlueprintTab == 1,
                    onOpenSheet = onOpenNightBlueprintSheet
                )
            }
        } else {
            items(activeBlockList, key = { it.id }) { block ->
                ScheduleBlockItemCard(
                    block = block,
                    isTomorrowView = selectedBlueprintTab == 1,
                    onToggle = {
                        PixelHaptics.confirmCheck(view)
                        onToggleBlockCompletion(block)
                    },
                    onRollOverTomorrow = {
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
    }
}

// ==========================================
// --- BOTANICAL DESIGN SYSTEM COMPONENTS ---
// ==========================================

@Composable
private fun BotanicalTopHeader(
    userName: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val currentHour = LocalTime.now().hour
    val greeting = when (currentHour) {
        in 5..11 -> "☀️ Good Morning"
        in 12..17 -> "🌤️ Good Afternoon"
        else -> "🌙 Good Evening"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Notification Bell Button
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onNotificationClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    // Notification Active Pill Dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Profile / Settings Grid Button
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Apps,
                        contentDescription = "Profile & Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateNewTaskBanner(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create New Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You can create new task here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SummaryGridSection(
    targetWakeMinutes: Int,
    actualWakeMinutes: Int?,
    isWakeGoalMet: Boolean,
    totalStudyMinutes: Int,
    targetStudyMinutes: Int,
    workoutCompleted: Boolean,
    totalTargetSets: Int,
    workoutSetsCount: Int,
    equilibriumRatio: Float,
    onTuneTargets: () -> Unit
) {
    val wakeProgress = if (isWakeGoalMet) 1f else if (actualWakeMinutes != null) 0.5f else 0.15f
    val studyProgress = (totalStudyMinutes.toFloat() / targetStudyMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val workoutProgress = if (workoutCompleted) 1f else if (totalTargetSets > 0) (workoutSetsCount.toFloat() / totalTargetSets.toFloat()).coerceIn(0f, 1f) else 0f
    val eqProgress = equilibriumRatio.coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onTuneTargets) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Edit Daily Targets",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2x2 Grid with Circular Color Filling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Wake Anchor
            SummaryTile(
                modifier = Modifier.weight(1f),
                fillProgress = wakeProgress,
                fillColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Outlined.WbSunny,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Wake Anchor",
                value = actualWakeMinutes?.let { formatMinutesToTime(it) } ?: formatMinutesToTime(targetWakeMinutes)
            )

            // Card 2: Deep Study
            SummaryTile(
                modifier = Modifier.weight(1f),
                fillProgress = studyProgress,
                fillColor = ZenithColors.PastelLilacAccent,
                icon = Icons.Outlined.AutoStories,
                iconTint = ZenithColors.PastelLilacText,
                title = "Deep Study",
                value = "%.1fh".format(totalStudyMinutes / 60f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 3: Conditioning
            SummaryTile(
                modifier = Modifier.weight(1f),
                fillProgress = workoutProgress,
                fillColor = ZenithColors.PastelRoseAccent,
                icon = Icons.Outlined.FitnessCenter,
                iconTint = ZenithColors.PastelRoseText,
                title = "Conditioning",
                value = if (workoutCompleted) "Done" else if (workoutSetsCount > 0) "$workoutSetsCount Sets" else "Pending"
            )

            // Card 4: Equilibrium
            SummaryTile(
                modifier = Modifier.weight(1f),
                fillProgress = eqProgress,
                fillColor = ZenithColors.PastelAmberAccent,
                icon = Icons.Outlined.Bolt,
                iconTint = ZenithColors.PastelAmberText,
                title = "Equilibrium",
                value = "${(equilibriumRatio * 100).toInt()}%"
            )
        }
    }
}

@Composable
private fun SummaryTile(
    modifier: Modifier = Modifier,
    fillProgress: Float,
    fillColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = fillProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileFillProgress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular Indicator with Color Filling
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = fillColor.copy(alpha = 0.16f)
                val fillBg = fillColor.copy(alpha = 0.12f + (animatedProgress * 0.25f))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Inner circle color filling
                    drawCircle(
                        color = fillBg,
                        radius = (diameter / 2f) - (strokeWidth / 2f)
                    )

                    // Track background ring
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Dynamic sweep color filling arc
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = fillColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (animatedProgress >= 0.99f) fillColor else iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActivityWeeklyChartCard(
    historyLogs: List<DailyLogEntity>,
    todayDate: LocalDate,
    todayStudyMinutes: Int,
    targetStudyMinutes: Int
) {
    val dayOfWeek = todayDate.dayOfWeek.value % 7
    val startOfWeek = todayDate.minusDays(dayOfWeek.toLong())
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val logMap = historyLogs.associateBy { it.dateEpochDay }

    val weeklyMinutes = weekDays.map { date ->
        if (date == todayDate) todayStudyMinutes
        else logMap[date.toEpochDay()]?.totalStudyMinutes ?: 0
    }
    val totalWeeklyHours = weeklyMinutes.sum() / 60
    val totalWeeklyMinutesRemain = weeklyMinutes.sum() % 60
    val maxMinutes = (weeklyMinutes.maxOrNull() ?: 360).coerceAtLeast(360)

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${totalWeeklyHours}h ${totalWeeklyMinutesRemain}m on this week.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 7-Day Vertical Pill Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekDays.forEachIndexed { idx, date ->
                    val isToday = date == todayDate
                    val mins = weeklyMinutes[idx]
                    val fraction = (mins.toFloat() / maxMinutes.toFloat()).coerceIn(0.12f, 1f)

                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "barHeight$idx"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
                                drawLine(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(animatedFraction)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }
                            }
                        }

                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// --- COMPACT & HIGHLY EDITABLE STUDY FOCUS CARD ---
@Composable
private fun BotanicalFocusMonitorCard(
    loggedMinutes: Int,
    targetMinutes: Int,
    isTimerRunning: Boolean,
    activeSeconds: Int,
    onToggleTimer: () -> Unit,
    onQuickAddMinutes: (Int) -> Unit,
    onOpenEdit: () -> Unit
) {
    val progress = (loggedMinutes.toFloat() / targetMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Category Label + Edit Button + Pomodoro Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PILLAR II • DEEP STUDY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onOpenEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Study Session",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) ZenithColors.PastelRoseText else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isTimerRunning) "%02d:%02d".format(activeSeconds / 60, activeSeconds % 60) else "POMODORO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Middle: Big Duration Readout & Target Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${loggedMinutes / 60}h ${loggedMinutes % 60}m",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Target: ${targetMinutes / 60}h (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Compact Linear Progress Bar with Rounded Ends
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )

            // Compact Quick Add & Edit Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    SuggestionChip(
                        onClick = { onQuickAddMinutes(mins) },
                        label = { Text("+$mins m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                SuggestionChip(
                    onClick = onOpenEdit,
                    label = { Text("✏️ Edit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

// --- COMPACT STUDY SESSION & TARGET EDITOR DIALOG ---
@Composable
private fun CompactStudyEditorDialog(
    currentLoggedMinutes: Int,
    currentTargetMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (newLoggedMinutes: Int, newTargetMinutes: Int) -> Unit
) {
    var loggedMins by remember { mutableIntStateOf(currentLoggedMinutes) }
    var targetMins by remember { mutableIntStateOf(currentTargetMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "EDIT STUDY TIME & TARGET",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Logged Time Stepper
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Logged Study Time Today:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${loggedMins / 60}h ${loggedMins % 60}m ($loggedMins mins)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { loggedMins = (loggedMins - 15).coerceAtLeast(0) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-15m", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { loggedMins += 15 },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+15m", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { loggedMins += 30 },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+30m", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Target Hours Stepper
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Today's Study Goal / Target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${targetMins / 60}h Target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4, 6, 8, 10).forEach { hrs ->
                                FilterChip(
                                    selected = targetMins == hrs * 60,
                                    onClick = { targetMins = hrs * 60 },
                                    label = { Text("${hrs}h", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(loggedMins, targetMins) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE CHANGES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}

@Composable
private fun BotanicalWakeAnchorCard(
    targetMinutes: Int,
    actualMinutes: Int?,
    onLogWakeNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PILLAR I • CIRCADIAN WAKE ANCHOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${formatMinutesToTime(targetMinutes)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (actualMinutes != null) {
                    val deviation = actualMinutes - targetMinutes
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (deviation <= 15) MaterialTheme.colorScheme.primaryContainer else ZenithColors.PastelRose
                    ) {
                        Text(
                            text = if (deviation <= 0) "MET (${-deviation}m early)" else if (deviation <= 15) "MET (+${deviation}m)" else "LATE (+${deviation}m)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (deviation <= 15) MaterialTheme.colorScheme.onPrimaryContainer else ZenithColors.PastelRoseText
                        )
                    }
                }
            }

            if (actualMinutes == null) {
                Button(
                    onClick = onLogWakeNow,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("LOG WAKE-UP NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                Text(
                    text = "Logged today at ${formatMinutesToTime(actualMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BotanicalTrainingCard(
    modality: WorkoutModality,
    isCompleted: Boolean,
    entries: List<WorkoutEntryEntity>,
    onSwitchModality: (WorkoutModality) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onOpenAddExercise: () -> Unit,
    onIncrementSet: (WorkoutEntryEntity) -> Unit,
    onDeleteSet: (WorkoutEntryEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PILLAR III • PHYSICAL CONDITIONING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = modality.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                FilterChip(
                    selected = isCompleted,
                    onClick = { onToggleComplete(!isCompleted) },
                    label = {
                        Text(
                            if (isCompleted) "COMPLETED" else "MARK DONE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            // Modality Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutModality.entries.forEach { item ->
                    val selected = item == modality
                    FilterChip(
                        selected = selected,
                        onClick = { onSwitchModality(item) },
                        label = {
                            Text(
                                if (item == WorkoutModality.GYM) "GYM HYPERTROPHY" else "CALISTHENICS / HOME",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            // Exercise List
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${entry.completedSets}/${entry.targetSets} sets • ${entry.repsAndLoad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = { onIncrementSet(entry) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add Set", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onDeleteSet(entry) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenAddExercise,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScheduleBlockItemCard(
    block: ScheduleBlockEntity,
    isTomorrowView: Boolean,
    onToggle: () -> Unit,
    onRollOverTomorrow: () -> Unit,
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

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier.fillMaxWidth()
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🕒 $startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Edit, Rollover, and Checkbox Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditBlock, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Block",
                        tint = cardText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!isTomorrowView) {
                    IconButton(onClick = onRollOverTomorrow, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = "Rollover to tomorrow",
                            tint = cardText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Checkbox(
                    checked = block.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = cardText.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyBlueprintPrompt(
    isTomorrow: Boolean,
    onOpenSheet: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (isTomorrow) "No blocks planned for tomorrow yet" else "No time blocks active today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Lock in your schedule blocks now to prevent drift.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onOpenSheet,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("+ PLAN TIME BLOCKS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
