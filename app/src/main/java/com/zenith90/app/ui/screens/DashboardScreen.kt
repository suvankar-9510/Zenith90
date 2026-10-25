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
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.*
import com.zenith90.app.ui.components.HapticTimeSlider
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate

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
    onNavigateToBlueprint: (() -> Unit)? = null
) {
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
        var selectedSubject by remember { mutableStateOf("QUANTUM MECH") }
        AlertDialog(
            onDismissRequest = onDiscardPomodoro,
            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = ZenithColors.AmberStart) },
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
                        listOf("QUANTUM", "STAT PHYS", "ELECTRODYN", "REVISION").forEach { tag ->
                            FilterChip(
                                selected = selectedSubject == tag,
                                onClick = { selectedSubject = tag },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    PixelHaptics.confirmCheck(view)
                    onConfirmPomodoro(state.pendingPomodoroMinutes, selectedSubject)
                }) {
                    Text("CONFIRM & LOG", style = MaterialTheme.typography.labelSmall)
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
            icon = { Icon(Icons.Outlined.Verified, contentDescription = null, tint = ZenithColors.AmberStart) },
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
                                onClick = { selectedSubject = subj },
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
                                onClick = { focusIntensity = intensity },
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
                    }
                ) {
                    Text("VERIFY & CREDIT +${boostMins}M", style = MaterialTheme.typography.labelSmall)
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
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = targetDaysInput,
                        onValueChange = { targetDaysInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days Until Target Exam / Deadline") },
                        placeholder = { Text("87") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                Button(onClick = {
                    PixelHaptics.heavyUnlock(view)
                    val days = targetDaysInput.toLongOrNull() ?: 87L
                    val targetDate = LocalDate.now().plusDays(days)
                    val studyMins = (studyHoursInput * 60).toInt()
                    val wakeMins = wakeMinsInput.toInt()

                    onUpdateUserSettings(examTitleInput, targetDate, wakeMins, studyMins)
                    showExamConfigDialog = false
                }) {
                    Text("LOCK IN MY GOALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                    text = "EDIT TODAY'S TARGETS",
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
                        title = "Target Wake-Up Time",
                        formattedValue = formatMinutesToTime(todayWakeMins.toInt()),
                        icon = Icons.Outlined.WbSunny
                    )

                    HapticTimeSlider(
                        value = todayStudyHrs,
                        onValueChange = { todayStudyHrs = it },
                        valueRange = 1f..14f,
                        stepMinutes = 1,
                        title = "Target Deep Study (Hours)",
                        formattedValue = "%.1f Hours (%d mins)".format(todayStudyHrs, (todayStudyHrs * 60).toInt()),
                        icon = Icons.Outlined.AutoStories
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    PixelHaptics.confirmCheck(view)
                    onUpdateTodayTargets(todayWakeMins.toInt(), (todayStudyHrs * 60).toInt())
                    showTargetEditDialog = false
                }) {
                    Text("SAVE TODAY'S TARGETS", style = MaterialTheme.typography.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetEditDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG 4: ADD CUSTOM WORKOUT EXERCISE ---
    if (showAddExerciseDialog) {
        var exerciseName by remember { mutableStateOf("") }
        var targetSets by remember { mutableStateOf("4") }
        var repsLoad by remember { mutableStateOf("8-10 reps @ 75kg") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("ADD WORKOUT EXERCISE", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") },
                        placeholder = { Text("e.g. Barbell Bench Press") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val suggestions = if (dayLog.workoutModality == WorkoutModality.GYM) {
                            listOf("Bench Press", "Squat", "Deadlift", "Shoulder Press")
                        } else {
                            listOf("Dand / Pushups", "Bethak / Squats", "Weighted Pull-Ups", "Dips")
                        }
                        suggestions.take(3).forEach { name ->
                            AssistChip(
                                onClick = { exerciseName = name },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = targetSets,
                            onValueChange = { targetSets = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Sets") },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = repsLoad,
                            onValueChange = { repsLoad = it },
                            label = { Text("Reps & Load / Notes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
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
                    enabled = exerciseName.isNotBlank()
                ) {
                    Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall)
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
                    text = state.userProfile?.displayName ?: "Zenith90 Scholar",
                    style = MaterialTheme.typography.titleMedium
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CONFIGURE EXAM & TARGETS", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    PixelHaptics.confirmCheck(view)
                    onTriggerCloudSync()
                    showProfileDialog = false
                }) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SYNC TO FIREBASE", style = MaterialTheme.typography.labelSmall)
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 100.dp)
    ) {
        // --- 1. BRAND & EXAM COUNTDOWN HEADER ---
        item {
            ExamCountdownHeader(
                examTitle = state.examTitle,
                daysRemaining = state.daysRemaining,
                readinessPercent = state.transformationReadinessPercent,
                streakDays = state.overallStreakDays,
                userName = state.userProfile?.displayName ?: "Scholar",
                onOpenProfile = {
                    PixelHaptics.crispTick(view)
                    showProfileDialog = true
                },
                onEditExam = {
                    PixelHaptics.crispTick(view)
                    showExamConfigDialog = true
                }
            )
        }

        // --- 2. TODAY'S 3-PILLAR EQUILIBRIUM CARD ---
        item {
            TodayThreePillarRingCard(
                dayLog = dayLog,
                onEditTargets = {
                    PixelHaptics.crispTick(view)
                    showTargetEditDialog = true
                }
            )
        }

        // --- 3. PILLAR I: 05:30 AM CIRCADIAN WAKE TRACKER ---
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

        // --- 4. PILLAR II: DEEP STUDY ENGINE & QUICK BOOSTS ---
        item {
            StudyFocusMonitorCard(
                loggedMinutes = dayLog.totalStudyMinutes,
                targetMinutes = dayLog.targetStudyMinutes,
                isTimerRunning = state.isFocusTimerRunning,
                activeSeconds = state.activeFocusSeconds,
                onToggleTimer = {
                    PixelHaptics.heavyUnlock(view)
                    onToggleFocusTimer()
                },
                onQuickAddMinutes = { mins ->
                    PixelHaptics.crispTick(view)
                    pendingBoostMinutes = mins
                }
            )
        }

        // --- 5. PILLAR III: PHYSICAL CONDITIONING (CUSTOMIZABLE EXERCISES) ---
        item {
            TrainingPillarCard(
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

        // --- 6. TIME-BLOCKED BLUEPRINT & SCHEDULE (TODAY & TOMORROW) ---
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        PixelHaptics.crispTick(view)
                        onOpenNightBlueprintSheet()
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ NIGHT PLANNER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Day Switcher Tabs (Today vs Tomorrow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            selectedBlueprintTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedBlueprintTab == 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
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
                            containerColor = if (selectedBlueprintTab == 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TOMORROW (${tomorrowBlocks.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Dedicated Blueprint Page Shortcut
                if (onNavigateToBlueprint != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                PixelHaptics.crispTick(view)
                                onNavigateToBlueprint()
                            },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "OPEN DEDICATED BLUEPRINT PAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

// --- SUB-COMPONENTS ---

@Composable
private fun ExamCountdownHeader(
    examTitle: String,
    daysRemaining: Long,
    readinessPercent: Float,
    streakDays: Int,
    userName: String,
    onOpenProfile: () -> Unit,
    onEditExam: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.clickable { onEditExam() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZenithLogo(sizeDp = 22)
                    Column {
                        Text(
                            text = examTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$daysRemaining DAYS REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.clickable { onOpenProfile() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${userName.uppercase()} • 🔥 $streakDays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Trajectory Progress Card
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
                            text = "90-DAY TRANSFORMATION READINESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Apex Exam Velocity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${"%.1f".format(readinessPercent)}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
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
private fun TodayThreePillarRingCard(
    dayLog: DailyLogEntity,
    onEditTargets: () -> Unit
) {
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
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S 3-PILLAR EQUILIBRIUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily Circadian • Focus • Physical Completion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditTargets) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Edit Targets", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${(dayLog.completionRatio * 100).toInt()}% TODAY",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PillarLegendRow(
                        "PILLAR I (WAKE)",
                        "Target: ${formatMinutesToTime(dayLog.targetWakeTimeMinutes)}",
                        ZenithColors.AmberStart
                    )
                    PillarLegendRow(
                        "PILLAR II (STUDY)",
                        "${dayLog.totalStudyMinutes / 60}h ${dayLog.totalStudyMinutes % 60}m / ${dayLog.targetStudyMinutes / 60}h",
                        ZenithColors.SageStart
                    )
                    PillarLegendRow(
                        "PILLAR III (TRAIN)",
                        if (dayLog.workoutCompleted) "COMPLETED" else "PENDING",
                        ZenithColors.VioletStart
                    )
                }

                // 3-Ring Visual Canvas
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        val spacing = 13.dp.toPx()

                        // Outer ring: Study
                        drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(brush = ZenithColors.StudyCompletionGradient, startAngle = -90f, sweepAngle = 360f * studyProgress, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))

                        // Middle ring: Wake
                        val midInset = spacing
                        drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset), size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(brush = ZenithColors.StreakGradient, startAngle = -90f, sweepAngle = 360f * wakeProgress, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset), size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))

                        // Inner ring: Workout
                        val innerInset = spacing * 2
                        drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(innerInset, innerInset), size = androidx.compose.ui.geometry.Size(size.width - innerInset * 2, size.height - innerInset * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(color = ZenithColors.VioletStart, startAngle = -90f, sweepAngle = 360f * workoutProgress, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(innerInset, innerInset), size = androidx.compose.ui.geometry.Size(size.width - innerInset * 2, size.height - innerInset * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    }
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PillarLegendRow(label: String, value: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "PILLAR I • CIRCADIAN WAKE ANCHOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Target: ${formatMinutesToTime(targetMinutes)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (actualMinutes != null) {
                    val deviation = actualMinutes - targetMinutes
                    Surface(
                        shape = CircleShape,
                        color = if (deviation <= 15) ZenithColors.SageStart.copy(alpha = 0.2f) else ZenithColors.CoralEnd.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (deviation <= 0) "MET (${-deviation}m early)" else if (deviation <= 15) "MET (+${deviation}m)" else "LATE (+${deviation}m)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (deviation <= 15) ZenithColors.SageStart else ZenithColors.CoralEnd
                        )
                    }
                }
            }

            if (actualMinutes == null) {
                Button(
                    onClick = onLogWakeNow,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LOG WAKE-UP NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
private fun StudyFocusMonitorCard(
    loggedMinutes: Int,
    targetMinutes: Int,
    isTimerRunning: Boolean,
    activeSeconds: Int,
    onToggleTimer: () -> Unit,
    onQuickAddMinutes: (Int) -> Unit
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
                    Text(text = "PILLAR II • DEEP STUDY ENGINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${loggedMinutes / 60}h ${loggedMinutes % 60}m / ${targetMinutes / 60}h Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) ZenithColors.CoralEnd else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTimerRunning) "%02d:%02d".format(activeSeconds / 60, activeSeconds % 60) else "POMODORO (25M)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick study boost buttons (+25m, +50m)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onQuickAddMinutes(25) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+25M BOOST", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onQuickAddMinutes(50) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+50M BOOST", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TrainingPillarCard(
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
                    Text(text = "PILLAR III • PHYSICAL CONDITIONING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = modality.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                FilterChip(
                    selected = isCompleted,
                    onClick = { onToggleComplete(!isCompleted) },
                    label = { Text(if (isCompleted) "COMPLETED" else "MARK DONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Modality Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutModality.entries.forEach { item ->
                    val selected = item == modality
                    FilterChip(
                        selected = selected,
                        onClick = { onSwitchModality(item) },
                        label = { Text(if (item == WorkoutModality.GYM) "GYM HYPERTROPHY" else "CALISTHENICS / HOME", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                AssistChip(
                    onClick = onOpenAddExercise,
                    label = { Text("+ EXERCISE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            // List of exercises
            if (entries.isEmpty()) {
                Text(
                    text = "No exercises logged today yet. Tap \"+ EXERCISE\" to track sets and load.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEach { entry ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${entry.repsAndLoad} • ${entry.completedSets}/${entry.targetSets} Sets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isCompleted) ZenithColors.SageStart else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onIncrementSet(entry) },
                                    enabled = entry.completedSets < entry.targetSets,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (entry.completedSets >= entry.targetSets) "DONE" else "+1 SET",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = { onDeleteSet(entry) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
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
    val bgColor by animateColorAsState(
        targetValue = if (block.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = block.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${formatMinutesToTime(block.startTimeMinutes)} (${block.durationMinutes}m)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (!block.examSubjectTag.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = block.examSubjectTag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isTomorrowView && !block.isCompleted) {
                    IconButton(onClick = onRollOverTomorrow) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Roll over to tomorrow",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDeleteBlock) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isTomorrow) "NO TASKS PLANNED FOR TOMORROW" else "NO SCHEDULE BLOCKS FOR TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isTomorrow) "Lock in tomorrow's objectives before bed." else "Start your morning by time-blocking your focus hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = onOpenSheet, shape = RoundedCornerShape(12.dp)) {
                Text(if (isTomorrow) "PLAN TOMORROW'S BLUEPRINT" else "CREATE TIME BLOCK", style = MaterialTheme.typography.labelSmall)
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
