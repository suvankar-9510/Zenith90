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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
    onSwitchWorkoutModality: (WorkoutModality) -> Unit,
    onToggleWorkoutCompleted: (Boolean) -> Unit,
    onAddWorkoutExercise: (name: String, targetSets: Int, repsAndLoad: String) -> Unit,
    onIncrementWorkoutSet: (WorkoutEntryEntity) -> Unit,
    onDeleteWorkoutSet: (WorkoutEntryEntity) -> Unit,
    onToggleBlockCompletion: (ScheduleBlockEntity) -> Unit,
    onRescheduleBlockTomorrow: (ScheduleBlockEntity) -> Unit,
    onDeleteScheduleBlock: (ScheduleBlockEntity) -> Unit,
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
            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = ZenithColors.MintAccent) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Text("CONFIRM & LOG", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
            icon = { Icon(Icons.Outlined.Verified, contentDescription = null, tint = ZenithColors.MintAccent) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Text("VERIFY & CREDIT +${boostMins}M", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBoostMinutes = null }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Text("LOCK IN MY GOALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Text("APPLY TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
                        color = ZenithColors.ForestGreenPrimary
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
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SYNC TO FIREBASE", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
            .background(ZenithColors.BotanicalBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 100.dp)
    ) {
        // --- 1. BOTANICAL TOP GREETING HEADER (Matches Center Screen) ---
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

        // --- 2. "CREATE NEW TASK / BLUEPRINT" HERO BANNER (Matches Center Screen) ---
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

        // --- 3. 2x2 "SUMMARY" GRID (Matches Center Screen) ---
        item {
            SummaryGridSection(
                targetWakeMinutes = dayLog.targetWakeTimeMinutes,
                actualWakeMinutes = dayLog.actualWakeTimeMinutes,
                totalStudyMinutes = dayLog.totalStudyMinutes,
                targetStudyMinutes = dayLog.targetStudyMinutes,
                workoutCompleted = dayLog.workoutCompleted,
                workoutSetsCount = workoutEntries.sumOf { it.completedSets },
                equilibriumRatio = dayLog.completionRatio,
                onTuneTargets = {
                    PixelHaptics.crispTick(view)
                    showTargetEditDialog = true
                }
            )
        }

        // --- 4. "ACTIVITY" WEEKLY BAR CHART (Matches Center Screen) ---
        item {
            ActivityWeeklyChartCard(
                historyLogs = state.fourMonthHistory,
                todayDate = state.todayDate,
                todayStudyMinutes = dayLog.totalStudyMinutes,
                targetStudyMinutes = dayLog.targetStudyMinutes
            )
        }

        // --- 5. PILLAR II: DEEP FOCUS ENGINE & POMODORO (Breathing animation) ---
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
                        color = ZenithColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        PixelHaptics.crispTick(view)
                        onOpenNightBlueprintSheet()
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = ZenithColors.ForestGreenPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("+ NIGHT PLANNER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ZenithColors.ForestGreenPrimary)
                    }
                }

                // Day Switcher Tabs (Today vs Tomorrow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZenithColors.BotanicalSurface)
                        .padding(4.dp)
                ) {
                    val todayActive = selectedBlueprintTab == 0
                    Button(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            selectedBlueprintTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (todayActive) ZenithColors.ForestGreenPrimary else Color.Transparent,
                            contentColor = if (todayActive) Color.White else ZenithColors.TextMuted
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
                            containerColor = if (!todayActive) ZenithColors.ForestGreenPrimary else Color.Transparent,
                            contentColor = if (!todayActive) Color.White else ZenithColors.TextMuted
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
                color = ZenithColors.TextMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ZenithColors.TextHeading
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Notification Bell Button with Notification Status Indicator
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onNotificationClick() },
                shape = CircleShape,
                color = ZenithColors.BotanicalSurface,
                border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = ZenithColors.TextHeading,
                        modifier = Modifier.size(22.dp)
                    )
                    // Notification Active Pill Dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ZenithColors.MintAccent)
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
                color = ZenithColors.BotanicalSurface,
                border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Apps,
                        contentDescription = "Profile & Settings",
                        tint = ZenithColors.TextHeading,
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
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Forest Green Circular Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ZenithColors.ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create New Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZenithColors.TextHeading
                )
                Text(
                    text = "You can create new task here",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenithColors.TextMuted
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = ZenithColors.TextHeading,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SummaryGridSection(
    targetWakeMinutes: Int,
    actualWakeMinutes: Int?,
    totalStudyMinutes: Int,
    targetStudyMinutes: Int,
    workoutCompleted: Boolean,
    workoutSetsCount: Int,
    equilibriumRatio: Float,
    onTuneTargets: () -> Unit
) {
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
                color = ZenithColors.TextHeading
            )
            IconButton(onClick = onTuneTargets) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Edit Daily Targets",
                    tint = ZenithColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Wake Target
            SummaryTile(
                modifier = Modifier.weight(1f),
                badgeColor = ZenithColors.PastelMint,
                icon = Icons.Outlined.WbSunny,
                iconTint = ZenithColors.PastelMintText,
                title = "Wake Anchor",
                value = actualWakeMinutes?.let { formatMinutesToTime(it) } ?: formatMinutesToTime(targetWakeMinutes)
            )

            // Card 2: Deep Study
            SummaryTile(
                modifier = Modifier.weight(1f),
                badgeColor = ZenithColors.PastelLilac,
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
            // Card 3: Conditioning / Workouts
            SummaryTile(
                modifier = Modifier.weight(1f),
                badgeColor = ZenithColors.PastelRose,
                icon = Icons.Outlined.FitnessCenter,
                iconTint = ZenithColors.PastelRoseText,
                title = "Conditioning",
                value = if (workoutCompleted) "Done" else if (workoutSetsCount > 0) "$workoutSetsCount Sets" else "Pending"
            )

            // Card 4: Equilibrium
            SummaryTile(
                modifier = Modifier.weight(1f),
                badgeColor = ZenithColors.PastelAmber,
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
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
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
                    color = ZenithColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZenithColors.TextHeading
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
    // Determine the 7 days of the current week (Sunday to Saturday)
    val dayOfWeek = todayDate.dayOfWeek.value % 7 // 0 for Sunday, 1 for Mon...
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
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Header: Dark green circle + Title "Activity" + Subtitle
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
                            .background(ZenithColors.ForestGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsRun,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZenithColors.TextHeading
                        )
                        Text(
                            text = "${totalWeeklyHours}h ${totalWeeklyMinutesRemain}m on this week.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenithColors.TextMuted
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ZenithColors.TextHeading,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 7-Day Vertical Pill Bar Chart (Matches Center Screen)
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
                            // Dotted/grid background line
                            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
                                drawLine(
                                    color = ZenithColors.BotanicalBorder,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // The Vertical Pill Bar
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(animatedFraction)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isToday) ZenithColors.ForestGreenPrimary
                                        else ZenithColors.PastelMint
                                    ),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (isToday) {
                                    // Top indicator dot on today's bar
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // Day label below (Sun, Mon, Tue...)
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isToday) ZenithColors.ForestGreenPrimary else ZenithColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotanicalFocusMonitorCard(
    loggedMinutes: Int,
    targetMinutes: Int,
    isTimerRunning: Boolean,
    activeSeconds: Int,
    onToggleTimer: () -> Unit,
    onQuickAddMinutes: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTimerRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PILLAR II • DEEP STUDY ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZenithColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${loggedMinutes / 60}h ${loggedMinutes % 60}m / ${targetMinutes / 60}h Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZenithColors.TextHeading
                    )
                }

                // Play / Stop Pill Button
                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) ZenithColors.PastelRoseText else ZenithColors.ForestGreenPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTimerRunning) "%02d:%02d".format(activeSeconds / 60, activeSeconds % 60) else "POMODORO (25M)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Quick study boost buttons (+25m, +50m) with Verification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onQuickAddMinutes(25) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
                ) {
                    Text("+25M BOOST", style = MaterialTheme.typography.labelSmall, color = ZenithColors.ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onQuickAddMinutes(50) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
                ) {
                    Text("+50M BOOST", style = MaterialTheme.typography.labelSmall, color = ZenithColors.ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BotanicalWakeAnchorCard(
    targetMinutes: Int,
    actualMinutes: Int?,
    onLogWakeNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
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
                        color = ZenithColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${formatMinutesToTime(targetMinutes)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZenithColors.TextHeading
                    )
                }
                if (actualMinutes != null) {
                    val deviation = actualMinutes - targetMinutes
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (deviation <= 15) ZenithColors.PastelMint else ZenithColors.PastelRose
                    ) {
                        Text(
                            text = if (deviation <= 0) "MET (${-deviation}m early)" else if (deviation <= 15) "MET (+${deviation}m)" else "LATE (+${deviation}m)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (deviation <= 15) ZenithColors.PastelMintText else ZenithColors.PastelRoseText
                        )
                    }
                }
            }

            if (actualMinutes == null) {
                Button(
                    onClick = onLogWakeNow,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("LOG WAKE-UP NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Text(
                    text = "Logged today at ${formatMinutesToTime(actualMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZenithColors.TextMuted
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
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
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
                        color = ZenithColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = modality.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZenithColors.TextHeading
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
                        .background(ZenithColors.BotanicalBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ZenithColors.TextHeading
                        )
                        Text(
                            text = "${entry.completedSets}/${entry.targetSets} sets • ${entry.repsAndLoad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenithColors.TextMuted
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
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = ZenithColors.TextMuted)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenAddExercise,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = ZenithColors.ForestGreenPrimary)
                Spacer(Modifier.width(6.dp))
                Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall, color = ZenithColors.ForestGreenPrimary, fontWeight = FontWeight.Bold)
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
    onDeleteBlock: () -> Unit
) {
    val startTimeStr = formatMinutesToTime(block.startTimeMinutes)
    val endTimeStr = formatMinutesToTime(block.startTimeMinutes + block.durationMinutes)

    // Select pastel tint based on category / priority (matching reference screen)
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Icon Avatar (Matches avatar in screenshot)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (block.isCompleted) TextDecoration.LineThrough else null,
                        color = if (block.isCompleted) cardText.copy(alpha = 0.5f) else ZenithColors.TextHeading
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

            // Rollover Arrow & Completion Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isTomorrowView) {
                    IconButton(onClick = onRollOverTomorrow, modifier = Modifier.size(34.dp)) {
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
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ZenithColors.ForestGreenPrimary,
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
        colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
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
                tint = ZenithColors.TextMuted,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (isTomorrow) "No blocks planned for tomorrow yet" else "No time blocks active today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ZenithColors.TextHeading
            )
            Text(
                text = "Lock in your schedule blocks now to prevent drift.",
                style = MaterialTheme.typography.bodySmall,
                color = ZenithColors.TextMuted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onOpenSheet,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
            ) {
                Text("+ PLAN TIME BLOCKS", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}
